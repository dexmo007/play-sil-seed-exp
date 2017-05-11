package models.daos

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import models.{HQUser, Roles}
import play.api.db.slick.DatabaseConfigProvider
import play.db.NamedDatabase

import scala.concurrent.Future

/**
  * Created by henri on 5/11/2017.
  */
class HQUserDAOImpl @Inject()(@NamedDatabase("users") protected val dbConfigProvider: DatabaseConfigProvider, loginInfoDAO: LoginInfoDAO) extends HQUserDAO {

  import driver.api._

  override def isSuperuser(userId: String): Future[Boolean] =
    db.run(superuserTable.filter(_.userId === userId).result.headOption).map(_.isDefined)

  override def isUserAdmin(userId: String): Future[Boolean] =
    db.run(userAdminTable.filter(_.userId === userId).result.headOption).map(_.isDefined)

  override def findGameAdmins(userId: String): Future[Seq[Roles.GameAdmin]] =
    db.run(gameAdminTable.filter(_.userId === userId).result).map(_ map {
      case (_, gameId, _) => Roles.GameAdmin(gameId)
    })

  /**
    * Finds a user by its login info.
    *
    * @param loginInfo The login info of the user to find.
    * @return The found user or None if no user for the given login info could be found.
    */
  override def find(loginInfo: LoginInfo): Future[Option[HQUser]] = {
    val query = for {
      dbLoginInfo <- loginInfoDAO.query(loginInfo)
      dbUserLoginInfo <- userLoginInfoTable.filter(_.loginInfoId === dbLoginInfo.id)
      dbUser <- hQUserTable.filter(_.id === dbUserLoginInfo.userID)
    } yield dbUser
    db.run(query.result.headOption) foMap { user =>
      findRoles(user.userID) map { roles =>
        HQUser(UUID.fromString(user.userID), loginInfo, user.nickname, user.email, user.avatarURL, user.activated, roles)
      }
    }
  }

  /**
    * Finds a user by its user ID.
    *
    * @param userID The ID of the user to find.
    * @return The found user or None if no user for the given ID could be found.
    */
  override def find(userID: UUID): Future[Option[HQUser]] = {
    val query = for {
      dbUser <- hQUserTable.filter(_.id === userID.toString)
      dbUserLoginInfo <- userLoginInfoTable.filter(_.userID === dbUser.id)
      dbLoginInfo <- loginInfoDAO.loginInfos.filter(_.id === dbUserLoginInfo.loginInfoId)
    } yield (dbUser, dbLoginInfo)
    db.run(query.result.headOption) foMap {
      case (user, loginInfo) => findRoles(user.userID) map { roles =>
        HQUser(UUID.fromString(user.userID),
          LoginInfo(loginInfo.providerID, loginInfo.providerKey),
          user.nickname, user.email, user.avatarURL, user.activated, roles)
      }
    }
  }

  /**
    * Saves a user.
    *
    * @param user The user to save.
    * @return The saved user.
    */
  override def save(user: HQUser): Future[HQUser] = {
    val dbUser = DBHQUser(user.userID.toString, user.nickname, user.email, user.avatarURL, user.activated)
    val dbLoginInfo = loginInfoDAO.DBLoginInfo(None, user.loginInfo.providerID, user.loginInfo.providerKey)
    // We don't have the LoginInfo id so we try to get it first.
    // If there is no LoginInfo yet for this user we retrieve the id on insertion.
    val loginInfoAction = {
      val retrieveLoginInfo = loginInfoDAO.loginInfos.filter(
        info => info.providerID === user.loginInfo.providerID &&
          info.providerKey === user.loginInfo.providerKey).result.headOption
      val insertLoginInfo = loginInfoDAO.loginInfos.returning(loginInfoDAO.loginInfos.map(_.id)).
        into((info, id) => info.copy(id = Some(id))) += dbLoginInfo
      for {
        loginInfoOption <- retrieveLoginInfo
        loginInfo <- loginInfoOption.map(DBIO.successful).getOrElse(insertLoginInfo)
      } yield loginInfo
    }
    // combine database actions to be run sequentially
    val actions = (for {
      _ <- hQUserTable.insertOrUpdate(dbUser)
      loginInfo <- loginInfoAction
      _ <- userLoginInfoTable += DBUserLoginInfo(dbUser.userID, loginInfo.id.get)
      _ <- superuserTable ++= user.roles.collect {
        case Roles.Superuser => (0L, dbUser.userID)
      }
      _ <- userAdminTable ++= user.roles.collect {
        case Roles.UserAdmin => (0L, dbUser.userID)
      }
      _ <- gameAdminTable ++= user.roles.collect {
        case Roles.GameAdmin(gameId) => (0L, gameId, dbUser.userID)
      }
    } yield ()).transactionally
    // run actions and return user afterwards
    db.run(actions) map (_ => user)
  }
}
