package models.daos

import java.util.UUID

import be.objectify.deadbolt.scala.models.Role
import com.mohiva.play.silhouette.api.LoginInfo
import models.{HQUser, Roles}

import scala.concurrent.Future

/**
  * Created by henri on 5/11/2017.
  */
trait HQUserDAO extends DAO {

  import driver.api._

  def isSuperuser(userId: String): Future[Boolean]

  def isUserAdmin(userId: String): Future[Boolean]

  def findGameAdmins(userId: String): Future[Seq[Roles.GameAdmin]]

  def findRoles(userId: String): Future[List[Role]] = {
    isSuperuser(userId) flatMap {
      case true => Future.successful(Roles.Superuser.withImplicits)
      case false =>
        isUserAdmin(userId).map(if (_) List(Roles.UserAdmin) else List[Role]()) flatMap {
          roles => findGameAdmins(userId) map (_ ++ roles)
        } map (_.toList)
    }
  }

  /**
    * Finds a user by its login info.
    *
    * @param loginInfo The login info of the user to find.
    * @return The found user or None if no user for the given login info could be found.
    */
  def find(loginInfo: LoginInfo): Future[Option[HQUser]]

  /**
    * Finds a user by its user ID.
    *
    * @param userID The ID of the user to find.
    * @return The found user or None if no user for the given ID could be found.
    */
  def find(userID: UUID): Future[Option[HQUser]]

  /**
    * Saves a user.
    *
    * @param user The user to save.
    * @return The saved user.
    */
  def save(user: HQUser): Future[HQUser]

  case class DBHQUser(userID: String,
                      nickname: String,
                      email: String,
                      avatarURL: Option[String],
                      activated: Boolean)

  class HQUserTable(tag: Tag) extends Table[DBHQUser](tag, "user") {
    def id = column[String]("userID", O.PrimaryKey)

    def nickname = column[String]("nickname")

    def email = column[Option[String]]("email")

    def avatarURL = column[Option[String]]("avatarURL")

    def activated = column[Boolean]("activated")

    def * = (id, nickname, email, avatarURL, activated) <> (DBHQUser.tupled, DBHQUser.unapply)
  }

  protected val hQUserTable = TableQuery[HQUserTable]

  case class DBUserLoginInfo(userID: String, loginInfoId: Long)

  class UserLoginInfoTable(tag: Tag) extends Table[DBUserLoginInfo](tag, "userlogininfo") {
    def userID = column[String]("userID")

    def loginInfoId = column[Long]("loginInfoId")

    def * = (userID, loginInfoId) <> (DBUserLoginInfo.tupled, DBUserLoginInfo.unapply)
  }

  protected val userLoginInfoTable = TableQuery[UserLoginInfoTable]

  /**
    * Table that contains all the superusers' IDs
    */
  class SuperuserTable(tag: Tag) extends Table[(Long, String)](tag, "hq_superuser") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def userId = column[String]("user_id")

    override def * = (id, userId)
  }

  protected val superuserTable = TableQuery[SuperuserTable]

  class GameAdminTable(tag: Tag) extends Table[(Long, Int, String)](tag, "hq_game_admin") {

    def id = column[Long]("id", O.AutoInc, O.PrimaryKey)

    def gameId = column[Int]("game_id")

    def userId = column[String]("user_id")

    override def * = (id, gameId, userId)

  }

  protected val gameAdminTable = TableQuery[GameAdminTable]

  class UserAdminTable(tag: Tag) extends Table[(Long, String)](tag, "hq_admin") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def userId = column[String]("user_id")

    override def * = (id, userId)
  }

  protected val userAdminTable = TableQuery[UserAdminTable]

}
