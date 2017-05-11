package models.daos

import java.util.UUID
import javax.inject.Inject

import models.AuthToken
import org.joda.time.DateTime
import play.api.db.slick.DatabaseConfigProvider
import play.db.NamedDatabase

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by henri on 5/11/2017.
 */
class AuthTokenDAOImpl @Inject() (@NamedDatabase("users") protected val dbConfigProvider: DatabaseConfigProvider) extends AuthTokenDAO with DAO {

  import driver.api._

  import com.github.tototoshi.slick.PostgresJodaSupport._ // enables JodaTime as columns and filtering by times

  /**
   * Finds a token by its ID.
   *
   * @param id The unique token ID.
   * @return The found token or None if no token for the given ID could be found.
   */
  override def find(id: UUID): Future[Option[AuthToken]] =
    db.run(authTokenTable.filter(_.id === id.toString).result.headOption)
      .map(_.map(dbToken => AuthToken(UUID.fromString(dbToken.id), UUID.fromString(dbToken.userId), dbToken.expiry)))

  /**
   * Finds expired tokens.
   *
   * @param dateTime The current date time.
   */
  override def findExpired(dateTime: DateTime): Future[Seq[AuthToken]] =
    db.run(authTokenTable.filter(_.expiry < dateTime).result).map(_ map {
      case DBAuthToken(id, userId, expiry) => AuthToken(UUID.fromString(id), UUID.fromString(userId), expiry)
    })

  /**
   * Saves a token.
   *
   * @param token The token to save.
   * @return The saved token.
   */
  override def save(token: AuthToken): Future[AuthToken] = {
    val dbToken = DBAuthToken(token.id.toString, token.userID.toString, token.expiry)
    db.run((authTokenTable += dbToken).transactionally).map(_ => token)
  }

  /**
   * Removes the token for the given ID.
   *
   * @param id The ID for which the token should be removed.
   * @return A future to wait for the process to be completed.
   */
  override def remove(id: UUID): Future[Unit] =
    db.run(authTokenTable.filter(_.id === id.toString).delete).map(_ => ())

  case class DBAuthToken(
    id: String,
    userId: String,
    expiry: DateTime)

  class AuthTokenTable(tag: Tag) extends Table[DBAuthToken](tag, "auth_token") {

    def id = column[String]("id")

    def userId = column[String]("user_id")

    def expiry = column[DateTime]("expiry")

    override def * = (id, userId, expiry) <> (DBAuthToken.tupled, DBAuthToken.unapply)
  }

  val authTokenTable = TableQuery[AuthTokenTable]

}
