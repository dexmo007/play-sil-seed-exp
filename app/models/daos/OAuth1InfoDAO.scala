package models.daos

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OAuth1Info
import play.api.db.slick.DatabaseConfigProvider
import play.db.NamedDatabase

/**
  * Created by henri on 5/10/2017.
  */
class OAuth1InfoDAO @Inject()(@NamedDatabase("users") protected val dbConfigProvider: DatabaseConfigProvider, val loginInfoDAO: LoginInfoDAO)
  extends AbstractDelegableAuthInfoDAO[OAuth1Info] with DAO {

  import driver.api._

  override type Wrapper = DBOAuth1Info
  override type DBTable = OAuth1InfoTable

  override def wrap(t: OAuth1Info, id: Long): DBOAuth1Info =
    DBOAuth1Info(None, t.token, t.secret, id)

  override def updateAction(loginInfo: LoginInfo, authInfo: OAuth1Info) =
    subQuery(loginInfo)
      .map(info => (info.token, info.secret))
      .update((authInfo.token, authInfo.secret))

  case class DBOAuth1Info(id: Option[Long],
                          token: String,
                          secret: String,
                          loginInfoId: Long) extends IdWrapper[OAuth1Info] {

    override def unwrap = OAuth1Info(token, secret)

  }

  class OAuth1InfoTable(tag: Tag) extends Table[DBOAuth1Info](tag, "oauth1info") with IdWrappedTable[DBOAuth1Info] {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def token = column[String]("token")

    def secret = column[String]("secret")

    def loginInfoId = column[Long]("loginInfoId")

    def * = (id.?, token, secret, loginInfoId) <> (DBOAuth1Info.tupled, DBOAuth1Info.unapply)
  }


}
