package repository

import app.Account
import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import doobie.implicits._
import doobie.util.transactor.Transactor

import scala.concurrent.Future

class DataRepository(implicit transactor: Transactor[IO]) extends LazyLogging {
  def saveDirectCampaign(account: Account, campaignId: Long): Future[Unit] = {
    val query =
      sql"""INSERT INTO data.yandex_direct_argos
      (account_id, campaign_id)
      SELECT ${account.id}, $campaignId
      WHERE NOT exists(
      SELECT 1 FROM data.yandex_direct_argos WHERE account_id=${account.id} AND campaign_id=$campaignId
      )""".update.run

    query.transact(transactor).unsafeToFuture.asInstanceOf[Future[Unit]]
  }

  def deleteDirectCampaign(account: Account, campaignId: Long): Future[Unit] = {
    val query =
      sql"""DELETE FROM data.yandex_direct_argos WHERE account_id=${account.id} AND campaign_id=$campaignId""".update.run

    query.transact(transactor).unsafeToFuture.asInstanceOf[Future[Unit]]
  }

  def saveDirectKeyword(account: Account, campaignId: Long, adGroupId: Long, keywordId: Long): Future[Unit] = {
    val query =
      sql"""INSERT INTO data.yandex_direct_argos
              (account_id, campaign_id, ad_group_id, keyword_id)
              SELECT ${account.id}, $campaignId, $adGroupId, $keywordId
              WHERE NOT exists(
                  SELECT 1 FROM data.yandex_direct_argos
                  WHERE account_id=${account.id} AND keyword_id=$keywordId
              )""".update.run

    query.transact(transactor).unsafeToFuture.asInstanceOf[Future[Unit]]
  }

  def deleteDirectKeyword(account: Account, keywordId: Long): Future[Unit] = {
    val query =
      sql"""DELETE FROM data.yandex_direct_argos WHERE account_id=${account.id} AND keyword_id=$keywordId""".update.run

    query.transact(transactor).unsafeToFuture.asInstanceOf[Future[Unit]]
  }

  def saveAdwordsCampaign(account: Account, campaignId: Long): Future[Unit] = {
    val query =
      sql"""INSERT INTO data.google_adwords_argos
      (account_id, campaign_id)
      SELECT ${account.id}, $campaignId
      WHERE NOT exists(
      SELECT 1 FROM data.google_adwords_argos WHERE account_id=${account.id} AND campaign_id=$campaignId
      )""".update.run

    query.transact(transactor).unsafeToFuture.asInstanceOf[Future[Unit]]
  }

  def deleteAdwordsCampaign(account: Account, campaignId: Long): Future[Unit] = {
    val query =
      sql"""DELETE FROM data.google_adwords_argos WHERE account_id=${account.id} AND campaign_id=$campaignId""".update.run

    query.transact(transactor).unsafeToFuture.asInstanceOf[Future[Unit]]
  }

  def saveAdwordsKeyword(account: Account, campaignId: Long, adGroupId: Long, keywordId: Long): Future[Unit] = {
    val query =
      sql"""INSERT INTO data.google_adwords_argos
              (account_id, campaign_id, ad_group_id, keyword_id)
              SELECT ${account.id}, $campaignId, $adGroupId, $keywordId
              WHERE NOT exists(
                  SELECT 1 FROM data.google_adwords_argos
                  WHERE account_id=${account.id} AND ad_group_id=$adGroupId AND keyword_id=$keywordId
              )""".update.run

    query.transact(transactor).unsafeToFuture.asInstanceOf[Future[Unit]]
  }

  def deleteAdwordsKeyword(account: Account, adGroupId: Long, keywordId: Long): Future[Unit] = {
    val query =
      sql"""DELETE FROM data.google_adwords_argos WHERE account_id=${account.id} AND ad_group_id=$adGroupId AND keyword_id=$keywordId""".update.run

    query.transact(transactor).unsafeToFuture.asInstanceOf[Future[Unit]]
  }
}
