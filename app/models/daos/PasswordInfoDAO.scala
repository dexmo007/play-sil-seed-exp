package models.daos

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import play.api.db.slick.DatabaseConfigProvider
import play.db.NamedDatabase

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by henri on 5/10/2017.
 */
class PasswordInfoDAO @Inject() (@NamedDatabase("users") protected val dbConfigProvider: DatabaseConfigProvider, val loginInfoDAO: LoginInfoDAO)
  extends AbstractDelegableAuthInfoDAO[PasswordInfo] with DAO {

  import driver.api._

  override type DBWrapper = DBPasswordInfo
  override type DBTable = PasswordInfoTable

  override protected val tableQuery = TableQuery[PasswordInfoTable]

  override def wrap(t: PasswordInfo, loginInfoId: Long) =
    DBPasswordInfo(t.hasher, t.password, t.salt, loginInfoId)

  override def updateAction(loginInfo: LoginInfo, authInfo: PasswordInfo) =
    subQuery(loginInfo)
      .map(info => (info.hasher, info.password, info.salt))
      .update((authInfo.hasher, authInfo.password, authInfo.salt))

  //  private def query(loginInfo: LoginInfo) = for {
  //    dbLoginInfo <- loginInfoDAO.query(loginInfo)
  //    dbPwInfo <- passwordInfos if dbPwInfo.loginInfoId === dbLoginInfo.id
  //  } yield dbPwInfo
  //
  //  override def find(loginInfo: LoginInfo) =
  //    db.run(query(loginInfo).result.headOption).map(_.map(_.unwrap))
  //
  //  private def addAction(loginInfo: LoginInfo, authInfo: PasswordInfo) =
  //    loginInfoDAO.query(loginInfo).result.head.flatMap { dbLoginInfo =>
  //      passwordInfos +=
  //        DBPasswordInfo(authInfo.hasher, authInfo.password, authInfo.salt, dbLoginInfo.id.get)
  //    }.transactionally
  //
  //  override def add(loginInfo: LoginInfo, authInfo: PasswordInfo) =
  //    db.run(addAction(loginInfo, authInfo)).map(_ => authInfo)
  //
  //  private def subQuery(loginInfo: LoginInfo) =
  //    passwordInfos.filter(_.loginInfoId in loginInfoDAO.query(loginInfo).map(_.id))
  //
  //  private def updateAction(loginInfo: LoginInfo, authInfo: PasswordInfo) =
  //    subQuery(loginInfo)
  //      .map(dbpi => (dbpi.hasher, dbpi.password, dbpi.salt))
  //      .update((authInfo.hasher, authInfo.password, authInfo.salt))
  //
  //
  //  override def update(loginInfo: LoginInfo, authInfo: PasswordInfo) = {
  //    db.run(updateAction(loginInfo, authInfo).map(_ => authInfo))
  //  }
  //
  //  override def save(loginInfo: LoginInfo, authInfo: PasswordInfo) = {
  //    db.run(loginInfoDAO.query(loginInfo)
  //      .joinLeft(passwordInfos).on(_.id === _.loginInfoId)
  //      .result.head.flatMap {
  //      case (_, Some(_)) => updateAction(loginInfo, authInfo)
  //      case (_, None) => addAction(loginInfo, authInfo)
  //    }).map(_ => authInfo)
  //  }
  //
  //  override def remove(loginInfo: LoginInfo) =
  //    db.run(subQuery(loginInfo).delete).map(_ => ())

  case class DBPasswordInfo(
    hasher: String,
    password: String,
    salt: Option[String],
    loginInfoId: Long) extends IdWrapper[PasswordInfo] {
    def unwrap = PasswordInfo(hasher, password, salt)

    override def id: Option[Long] = None
  }

  class PasswordInfoTable(tag: Tag) extends Table[DBPasswordInfo](tag, "password_info") with IdWrappedTable {
    def hasher = column[String]("hasher")

    def password = column[String]("password")

    def salt = column[Option[String]]("salt")

    def loginInfoId = column[Long]("login_info_id")

    def * = (hasher, password, salt, loginInfoId) <> (DBPasswordInfo.tupled, DBPasswordInfo.unapply)
  }

  //  val passwordInfos = TableQuery[PasswordInfoTable]

}
