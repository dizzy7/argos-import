package repository

import app.ContextSystem
import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor

import scala.collection.concurrent.TrieMap

class AccountRepository(implicit transactor: Transactor[IO]) {
  private val adSystemSource: Map[ContextSystem, Int] = Map(ContextSystem.Direct -> 1, ContextSystem.Adwords -> 2)
  private val accounts = TrieMap[(ContextSystem, String), Option[Long]]()

  def findAccountId(login: String, adSystem: ContextSystem): Option[Long] = {
    val source = adSystemSource(adSystem)
    accounts.getOrElseUpdate((adSystem, login), {
      val source = adSystemSource(adSystem)
      val query = sql"SELECT id FROM account WHERE login=$login AND source=$source".query[Long].option
      query.transact(transactor).unsafeRunSync()
    })
  }
}
