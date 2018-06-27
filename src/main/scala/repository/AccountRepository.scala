package repository

import app.AdSystem
import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor

import scala.collection.concurrent.TrieMap

class AccountRepository(implicit transactor: Transactor[IO]) {
  private val adSystemSource = Map(AdSystem.Direct -> 1, AdSystem.Adwords -> 2)
  private val accounts = TrieMap[(AdSystem.Value, String), Option[Long]]()

  def findAccountId(login: String, adSystem: AdSystem.Value): Option[Long] = {
    accounts.getOrElseUpdate((adSystem, login), {
      val source = adSystemSource(adSystem)
      val query = sql"SELECT id FROM account WHERE login=$login AND source=$source".query[Long].option
      query.transact(transactor).unsafeRunSync()
    })
  }
}
