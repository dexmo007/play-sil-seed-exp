package models.daos

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OpenIDInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import play.api.db.slick.DatabaseConfigProvider
import play.db.NamedDatabase

import scala.concurrent.Future

/**
  * Created by henri on 5/11/2017.
  */
class OpenIDInfoDAO @Inject()(@NamedDatabase("users") protected val dbConfigProvider: DatabaseConfigProvider, val loginInfoDAO: LoginInfoDAO)
  extends DelegableAuthInfoDAO[OpenIDInfo] with DAO {

  import driver.api._

  def wrap(t: OpenIDInfo, loginInfoId: Long) =
    DBOpenIDInfo(t.id, loginInfoId)

  protected def query(loginInfo: LoginInfo) = for {
    dbLoginInfo <- loginInfoDAO.query(loginInfo)
    dbOpenIDInfo <- openIDInfoTable if dbOpenIDInfo.loginInfoId === dbLoginInfo.id
  } yield dbOpenIDInfo

  protected def addAction(loginInfo: LoginInfo, authInfo: OpenIDInfo) =
    loginInfoDAO.query(loginInfo).result.head.flatMap { dbLoginInfo =>
      DBIO.seq(
        openIDInfoTable += DBOpenIDInfo(authInfo.id, dbLoginInfo.id.get),
        openIDAttributeTable ++= authInfo.attributes.map {
          case (key, value) => DBOpenIDAttribute(authInfo.id, key, value)
        })
    }.transactionally

  protected def updateAction(loginInfo: LoginInfo, authInfo: OpenIDInfo) =
    query(loginInfo).result.head.flatMap { dbOpenIDInfo =>
      DBIO.seq(
        openIDInfoTable filter (_.id === dbOpenIDInfo.id) update dbOpenIDInfo.copy(id = authInfo.id),
        openIDAttributeTable.filter(_.id === dbOpenIDInfo.id).delete,
        openIDAttributeTable ++= authInfo.attributes.map {
          case (key, value) => DBOpenIDAttribute(authInfo.id, key, value)
        })
    }.transactionally

  override def find(loginInfo: LoginInfo): Future[Option[OpenIDInfo]] =
    db.run(query(loginInfo).joinLeft(openIDAttributeTable).on(_.id === _.id).result)
      .map { openIdInfos =>
        if (openIdInfos.isEmpty) None
        else {
          val attributes = openIdInfos.collect { case (_, Some(attr)) => (attr.key, attr.value) }.toMap
          Some(OpenIDInfo(openIdInfos.head._1.id, attributes))
        }
      }

  override def add(loginInfo: LoginInfo, authInfo: OpenIDInfo): Future[OpenIDInfo] =
    db.run(addAction(loginInfo, authInfo)).map(_ => authInfo)

  override def update(loginInfo: LoginInfo, authInfo: OpenIDInfo): Future[OpenIDInfo] =
    db.run(updateAction(loginInfo, authInfo)).map(_ => authInfo)

  override def save(loginInfo: LoginInfo, authInfo: OpenIDInfo): Future[OpenIDInfo] = db.run(
    loginInfoDAO.query(loginInfo).joinLeft(openIDInfoTable).on(_.id === _.loginInfoId)
      .result.head.flatMap {
      case (_, Some(_)) => updateAction(loginInfo, authInfo)
      case (_, None) => addAction(loginInfo, authInfo)
    }
  ).map(_ => authInfo)

  override def remove(loginInfo: LoginInfo): Future[Unit] = {
    // Use subquery workaround instead of join because slick only supports selecting
    // from a single table for update/delete queries (https://github.com/slick/slick/issues/684).
    val openIDInfoSubQuery = openIDInfoTable.filter(_.loginInfoId in loginInfoDAO.query(loginInfo).map(_.id))
    val attributeSubQuery = openIDAttributeTable.filter(_.id in openIDInfoSubQuery.map(_.id))
    db.run((openIDInfoSubQuery.delete andThen attributeSubQuery.delete).transactionally).map(_ => ())
  }

  case class DBOpenIDInfo(id: String,
                          loginInfoId: Long) extends IdWrapper[OpenIDInfo] {
    override def unwrap: OpenIDInfo = OpenIDInfo(id, Map.empty)
  }

  class OpenIDInfoTable(tag: Tag) extends Table[DBOpenIDInfo](tag, "openidinfo") with IdWrappedTable[DBOpenIDInfo] {
    def id = column[String]("id", O.PrimaryKey)

    def loginInfoId = column[Long]("logininfoid")

    def * = (id, loginInfoId) <> (DBOpenIDInfo.tupled, DBOpenIDInfo.unapply)
  }

  private val openIDInfoTable = TableQuery[OpenIDInfoTable]

  case class DBOpenIDAttribute(id: String,
                               key: String,
                               value: String)

  class OpenIDAttributeTable(tag: Tag) extends Table[DBOpenIDAttribute](tag, "openidattributes") {
    def id = column[String]("id")

    def key = column[String]("key")

    def value = column[String]("value")

    def * = (id, key, value) <> (DBOpenIDAttribute.tupled, DBOpenIDAttribute.unapply)
  }

  private val openIDAttributeTable = TableQuery[OpenIDAttributeTable]

}
