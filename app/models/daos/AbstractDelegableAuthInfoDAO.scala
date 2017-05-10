package models.daos

import com.mohiva.play.silhouette.api.{AuthInfo, LoginInfo}
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import slick.lifted.Rep

trait IdWrapper[+T] {

  def id: Option[Long]

  def loginInfoId: Long

  def unwrap: T

}

trait IdWrappedTable[T <: IdWrapper[T]] {
  def loginInfoId: Rep[Long]
}

/**
  * Created by henri on 5/11/2017.
  */
trait AbstractDelegableAuthInfoDAO[T <: AuthInfo] extends DelegableAuthInfoDAO[T] with DAO {

  import driver.api._

  val loginInfoDAO: LoginInfoDAO

  type Wrapper <: IdWrapper[T]

  type DBTable <: Table[Wrapper] with IdWrappedTable[Wrapper]

  val tableQuery = TableQuery[DBTable]

  def wrap(t: T, loginInfoId: Long): Wrapper

  private def query(loginInfo: LoginInfo) = for {
    li <- loginInfoDAO.query(loginInfo)
    ai <- tableQuery if ai.loginInfoId === li.id
  } yield ai

  protected def subQuery(loginInfo: LoginInfo) =
    tableQuery.filter(_.loginInfoId in loginInfoDAO.query(loginInfo).map(_.id))

  private def addAction(loginInfo: LoginInfo, authInfo: T) =
    loginInfoDAO.query(loginInfo).result.head.flatMap { dbLoginInfo =>
      tableQuery += wrap(authInfo, dbLoginInfo.id.get)
    }.transactionally

  def updateAction(loginInfo: LoginInfo, authInfo: T): DBIOAction[Int, NoStream, Effect.Write]

  override def find(loginInfo: LoginInfo) =
    db.run(query(loginInfo).result.headOption).map(_.map(_.unwrap))

  override def add(loginInfo: LoginInfo, authInfo: T) =
    db.run(addAction(loginInfo, authInfo)).map(_ => authInfo)

  override def update(loginInfo: LoginInfo, authInfo: T) =
    db.run(updateAction(loginInfo, authInfo)).map(_ => authInfo)

  override def save(loginInfo: LoginInfo, authInfo: T) = {
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
