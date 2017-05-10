package models.daos

import play.api.db.slick.HasDatabaseConfigProvider
import slick.driver.JdbcProfile

import scala.concurrent.Future

/**
  * Created by henri on 5/10/2017.
  */
trait DAO extends HasDatabaseConfigProvider[JdbcProfile] {

  implicit class FutureOption[T](fo: Future[Option[T]]) {
    def foMap[U](mapper: T => Future[U]): Future[Option[U]] = {
      fo.flatMap {
        case Some(t) =>
          mapper(t).map(Some(_))
        case None => Future.successful(None)
      }
    }
  }

}
