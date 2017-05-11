package models.daos

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import play.api.db.slick.DatabaseConfigProvider
import play.db.NamedDatabase

/**
  * Created by henri on 5/11/2017.
  */
class OAuth2InfoDAO @Inject()(@NamedDatabase("users") protected val dbConfigProvider: DatabaseConfigProvider, val loginInfoDAO: LoginInfoDAO)
  extends AbstractDelegableAuthInfoDAO[OAuth2Info] with DAO {

  import driver.api._

  override type DBWrapper = DBOAuth2Info

  override type DBTable = OAuth2InfoTable

  override def wrap(t: OAuth2Info, loginInfoId: Long) =
    DBOAuth2Info(None, t.accessToken, t.tokenType, t.expiresIn, t.refreshToken, loginInfoId)

  override def updateAction(loginInfo: LoginInfo, authInfo: OAuth2Info) =
    subQuery(loginInfo)
      .map(info => (info.accessToken, info.tokenType, info.expiresIn, info.refreshToken))
      .update((authInfo.accessToken, authInfo.tokenType, authInfo.expiresIn, authInfo.refreshToken))

  case class DBOAuth2Info(id: Option[Long],
                          accessToken: String,
                          tokenType: Option[String],
                          expiresIn: Option[Int],
                          refreshToken: Option[String],
                          loginInfoId: Long) extends IdWrapper[OAuth2Info] {
    override def unwrap: OAuth2Info = OAuth2Info(accessToken, tokenType, expiresIn, refreshToken)
  }

  class OAuth2InfoTable(tag: Tag) extends Table[DBOAuth2Info](tag, "oauth2info") with IdWrappedTable[DBOAuth2Info] {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def accessToken = column[String]("accesstoken")

    def tokenType = column[Option[String]]("tokentype")

    def expiresIn = column[Option[Int]]("expiresin")

    def refreshToken = column[Option[String]]("refreshtoken")

    def loginInfoId = column[Long]("logininfoid")

    def * = (id.?, accessToken, tokenType, expiresIn, refreshToken, loginInfoId) <> (DBOAuth2Info.tupled, DBOAuth2Info.unapply)
  }


}
