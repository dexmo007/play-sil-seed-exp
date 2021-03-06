package models.daos

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import play.api.db.slick.DatabaseConfigProvider
import play.db.NamedDatabase

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by henri on 5/10/2017.
 */
class LoginInfoDAO @Inject() (@NamedDatabase("users") protected val dbConfigProvider: DatabaseConfigProvider) extends DAO {

  import driver.api._

  def findId(loginInfo: LoginInfo): Future[Option[Long]] = {
    db.run(query(loginInfo).result.head.map(_.id))
  }

  case class DBLoginInfo(
    id: Option[Long],
    providerID: String,
    providerKey: String)

  class LoginInfoTable(tag: Tag) extends Table[DBLoginInfo](tag, "login_info") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def providerID = column[String]("provider_id")

    def providerKey = column[String]("provider_key")

    def * = (id.?, providerID, providerKey) <> (DBLoginInfo.tupled, DBLoginInfo.unapply)
  }

  val loginInfos = TableQuery[LoginInfoTable]

  def query(loginInfo: LoginInfo) =
    loginInfos.filter(dbLoginInfo => dbLoginInfo.providerID === loginInfo.providerID && dbLoginInfo.providerKey === loginInfo.providerKey)

}
