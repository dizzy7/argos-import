package repository

import app.{Application, ContextSystem}
import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import doobie.implicits._
import doobie.util.transactor.Transactor.Aux

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AccountRepository extends LazyLogging {
  implicit val transactor: Aux[IO, Unit] = Application.metaTransactor
  private val adSystemSource: Map[ContextSystem, Int] = Map(ContextSystem.Direct -> 1, ContextSystem.Adwords -> 2)
  private val accounts = TrieMap[(ContextSystem, String), Option[Long]]()

  def findAccountId(login: String, adSystem: ContextSystem): Future[Option[Long]] = {
    val source = adSystemSource(adSystem)
    accounts.get((adSystem, login)) match {
      case Some(optionId) =>
        logger.info("Account hit")
        Future.successful(optionId)
      case None =>
        logger.info("Account miss")
        val query = sql"SELECT id FROM account WHERE login=$login AND source=$source".query[Long].option
        query.transact(transactor).unsafeToFuture().map { result =>
          accounts((adSystem, login)) = result
          result
        }
    }
  }
}
