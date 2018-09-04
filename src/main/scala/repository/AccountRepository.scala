package repository

import app.{Application, ContextSystem}
import com.typesafe.scalalogging.LazyLogging
import doobie.implicits._

import scala.collection.concurrent.TrieMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AccountRepository extends LazyLogging {
  private val adSystemSource: Map[ContextSystem, Int] = Map(ContextSystem.Direct -> 1, ContextSystem.Adwords -> 2)
  private val accounts = TrieMap[(ContextSystem, String), Option[Long]]()

  def findAccountId(login: String, adSystem: ContextSystem): Future[Option[Long]] = {
    val source = adSystemSource(adSystem)
    accounts.get((adSystem, login)) match {
      case Some(optionId) =>
        Future.successful(optionId)
      case None =>
        val query = sql"SELECT id FROM account WHERE login=$login AND source=$source".query[Long].option
        query.transact(Application.metaTransactor).unsafeToFuture().map { result =>
          accounts((adSystem, login)) = result
          result
        }
    }
  }
}
