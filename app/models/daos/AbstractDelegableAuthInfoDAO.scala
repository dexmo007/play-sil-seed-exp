package models.daos

import com.mohiva.play.silhouette.api.{ AuthInfo, LoginInfo }
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import slick.lifted.Rep

import scala.concurrent.ExecutionContext.Implicits.global
import scala.reflect.ClassTag

trait IdWrapper[+T] {

  def id: Option[Long]

  def loginInfoId: Long

  def unwrap: T

}

trait IdWrappedTable {
  def loginInfoId: Rep[Long]
}

/**
 * Created by henri on 5/11/2017.
 */
abstract class AbstractDelegableAuthInfoDAO[A <: AuthInfo: ClassTag] extends DelegableAuthInfoDAO[A] with DAO {

  import driver.api._

  val loginInfoDAO: LoginInfoDAO

  type DBWrapper <: IdWrapper[A]

  type DBTable <: Table[DBWrapper] with IdWrappedTable

  //  protected def tableQuery(implicit ct: ClassTag[DBTable]) = TableQuery[DBTable]

  protected val tableQuery: TableQuery[DBTable]

  def wrap(t: A, loginInfoId: Long): DBWrapper

  protected def query(loginInfo: LoginInfo) = for {
    li <- loginInfoDAO.query(loginInfo)
    ai <- tableQuery if ai.loginInfoId === li.id
  } yield ai

  protected def subQuery(loginInfo: LoginInfo) =
    tableQuery.filter(_.loginInfoId in loginInfoDAO.query(loginInfo).map(_.id))

  private def addAction(loginInfo: LoginInfo, authInfo: A) =
    loginInfoDAO.query(loginInfo).result.head.flatMap { dbLoginInfo =>
      tableQuery += wrap(authInfo, dbLoginInfo.id.get)
    }.transactionally

  def updateAction(loginInfo: LoginInfo, authInfo: A): DBIOAction[Int, NoStream, Effect.Write]

  override def find(loginInfo: LoginInfo) =
    db.run(query(loginInfo).result.headOption).map(_.map(_.unwrap))

  override def add(loginInfo: LoginInfo, authInfo: A) =
    db.run(addAction(loginInfo, authInfo)).map(_ => authInfo)

  override def update(loginInfo: LoginInfo, authInfo: A) =
    db.run(updateAction(loginInfo, authInfo)).map(_ => authInfo)

  override def save(loginInfo: LoginInfo, authInfo: A) = {
    val action = loginInfoDAO.query(loginInfo).joinLeft(tableQuery).on(_.id === _.loginInfoId)
      .result.head.flatMap {
        case (_, Some(_)) => updateAction(loginInfo, authInfo)
        case (_, None) => addAction(loginInfo, authInfo)
      }.transactionally
    db.run(action).map(_ => authInfo)
  }

  override def remove(loginInfo: LoginInfo) =
    db.run(subQuery(loginInfo).delete).map(_ => ())
}
