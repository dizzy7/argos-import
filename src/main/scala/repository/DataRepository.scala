package repository

import app.Application.executionContext
import app.StatisticAccount
import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import doobie.implicits._
import doobie.util.transactor.Transactor

import scala.concurrent.Future

class DataRepository(implicit transactor: Transactor[IO]) extends LazyLogging {
  def saveDirectCampaign(account: StatisticAccount, campaignId: Long): Future[Unit] = {
    val query =
      sql"""INSERT INTO data.yandex_direct_argos
      (account_id, campaign_id)
      SELECT ${account.id}, $campaignId
      WHERE NOT exists(
      SELECT 1 FROM data.yandex_direct_argos WHERE account_id=${account.id} AND campaign_id=$campaignId
      )""".update.run

    query.transact(transactor).unsafeToFuture.map(_ => ())
  }

  def deleteDirectCampaign(account: StatisticAccount, campaignId: Long): Future[Unit] = {
    val query =
      sql"""DELETE FROM data.yandex_direct_argos WHERE account_id=${account.id} AND campaign_id=$campaignId""".update.run

    query.transact(transactor).unsafeToFuture.map(_ => ())
  }

  def saveDirectKeyword(account: StatisticAccount, campaignId: Long, adGroupId: Long, keywordId: Long): Future[Unit] = {
    val query =
      sql"""INSERT INTO data.yandex_direct_argos
              (account_id, campaign_id, ad_group_id, keyword_id)
              SELECT ${account.id}, $campaignId, $adGroupId, $keywordId
              WHERE NOT exists(
                  SELECT 1 FROM data.yandex_direct_argos
                  WHERE account_id=${account.id} AND keyword_id=$keywordId
              )""".update.run

    query.transact(transactor).unsafeToFuture.map(_ => ())
  }

  def deleteDirectKeyword(account: StatisticAccount, keywordId: Long): Future[Unit] = {
    val query =
      sql"""DELETE FROM data.yandex_direct_argos WHERE account_id=${account.id} AND keyword_id=$keywordId""".update.run

    query.transact(transactor).unsafeToFuture.map(_ => ())
  }

  def saveAdwordsCampaign(account: StatisticAccount, campaignId: Long): Future[Unit] = {
    val query =
      sql"""INSERT INTO data.google_adwords_argos
      (account_id, campaign_id)
      SELECT ${account.id}, $campaignId
      WHERE NOT exists(
      SELECT 1 FROM data.google_adwords_argos WHERE account_id=${account.id} AND campaign_id=$campaignId
      )""".update.run

    query.transact(transactor).unsafeToFuture.map(_ => ())
  }

  def deleteAdwordsCampaign(account: StatisticAccount, campaignId: Long): Future[Unit] = {
    val query =
      sql"""DELETE FROM data.google_adwords_argos WHERE account_id=${account.id} AND campaign_id=$campaignId""".update.run

    query.transact(transactor).unsafeToFuture.map(_ => ())
  }

  def saveAdwordsKeyword(account: StatisticAccount, campaignId: Long, adGroupId: Long, keywordId: Long): Future[Unit] = {
    val query =
      sql"""INSERT INTO data.google_adwords_argos
              (account_id, campaign_id, ad_group_id, keyword_id)
              SELECT ${account.id}, $campaignId, $adGroupId, $keywordId
              WHERE NOT exists(
                  SELECT 1 FROM data.google_adwords_argos
                  WHERE account_id=${account.id} AND ad_group_id=$adGroupId AND keyword_id=$keywordId
              )""".update.run

    query.transact(transactor).unsafeToFuture.map(_ => ())
  }

  def deleteAdwordsKeyword(account: StatisticAccount, adGroupId: Long, keywordId: Long): Future[Unit] = {
    val query =
      sql"""DELETE FROM data.google_adwords_argos WHERE account_id=${account.id} AND ad_group_id=$adGroupId AND keyword_id=$keywordId""".update.run

    query.transact(transactor).unsafeToFuture.map(_ => ())
  }
}
