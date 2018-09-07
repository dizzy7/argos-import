package stream

import akka.stream.ActorAttributes.supervisionStrategy
import akka.stream.Supervision
import akka.stream.alpakka.amqp.scaladsl.CommittableIncomingMessage
import akka.stream.scaladsl._
import akka.{Done, NotUsed}
import app.ContextSystem.{Adwords, Direct}
import app.{ContextSystem, StatisticAccount}
import com.typesafe.scalalogging.LazyLogging
import config.RabbitmqConfig
import io.circe.generic.auto._
import io.circe.parser.decode
import rabbit.Connection
import repository.{AccountRepository, DataRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ImportFlow extends LazyLogging {

  case class Message(
    accountId: AccountInfo,
    id: Long,
    `type`: String,
    changeType: String,
    entityType: Option[String],
    adGroupId: Option[Long],
    campaignId: Option[Long]
  )

  case class AccountInfo(adSystem: String, accountId: String)

  type DecodedMessage = (CommittableIncomingMessage, Either[Exception, Message])
  type DecodedMessageWithAccount = (CommittableIncomingMessage, Either[Exception, (Message, StatisticAccount)])
  type FinalResult = (CommittableIncomingMessage, Either[Exception, Unit])

  val accountRepository = new AccountRepository()
  val dataRepository = new DataRepository()

  val decider: Supervision.Decider = { e =>
    logger.error(e.toString)
    Supervision.Resume
  }

  def queueSource(config: RabbitmqConfig): Source[CommittableIncomingMessage, NotUsed] = {
    val connection = new Connection(config)
    connection.createSource()
  }

  val decodeJson: Flow[CommittableIncomingMessage, DecodedMessage, NotUsed] =
    Flow[CommittableIncomingMessage].map[DecodedMessage]({ m =>
      val messageString = m.message.bytes.decodeString("UTF-8")
      (m, decode[Message](messageString))
    }).withAttributes(supervisionStrategy(decider))

  val findStatisticAccount: Flow[DecodedMessage, DecodedMessageWithAccount, NotUsed] =
    Flow[DecodedMessage].mapAsyncUnordered[DecodedMessageWithAccount](60) {
      case (m, Right(message)) =>
        val systemOption = ContextSystem(message.accountId.adSystem)
        systemOption match {
          case Some(system) =>
            accountRepository.findAccountId(message.accountId.accountId, system).map {
              case Some(id) => (m, Right(message, StatisticAccount(id, system)))
              case None => (m, Left(new RuntimeException("Аккаунт не найден")))
            }
          case None =>
            Future.successful((m, Left(new RuntimeException("Неизвестный тип аккаунта"))))
        }
      case (m, Left(e)) =>
        Future.successful((m, Left(e)))
    }.withAttributes(supervisionStrategy(decider))

  val processSystem: Flow[DecodedMessageWithAccount, FinalResult, NotUsed] =
    Flow[DecodedMessageWithAccount].mapAsyncUnordered[FinalResult](60) {
      case (msg, Left(e)) => Future.successful((msg, Left(e)))
      case (msg, Right(message@(_: Message, StatisticAccount(_, Direct)))) =>
        directProcess(message).map(u => (msg, Right()))
      case (msg, Right(message@(_: Message, StatisticAccount(_, Adwords)))) =>
        adwordsProcess(message).map(u => (msg, Right()))
      case (msg, _) => Future.successful((msg, Left(new RuntimeException("Не найден обработчик для сообщения"))))
    }.withAttributes(supervisionStrategy(decider))

  def directProcess(message: (Message, StatisticAccount)): Future[Unit] = {
    message match {
      case (Message(_, id, "campaigns", "downloaded", _, _, _), account) =>
        dataRepository.saveDirectCampaign(account, id)
      case (Message(_, id, "campaigns", "deleted", _, _, _), account) =>
        dataRepository.deleteDirectCampaign(account, id)
      case (Message(_, id, "keywords", "downloaded", _, Some(adGroupId), Some(campaignId)), account) =>
        dataRepository.saveDirectKeyword(account, campaignId, adGroupId, id)
      case (Message(_, id, "keywords", "deleted", _, _, _), account) =>
        dataRepository.deleteDirectKeyword(account, id)
      case (msg: Message, _) =>
        Future.failed(new RuntimeException("Не обработано: " + msg.toString))
    }
  }

  def adwordsProcess(message: (Message, StatisticAccount)): Future[Unit] = {
    message match {
      case (Message(_, id, "campaigns", "downloaded", _, _, _), account) =>
        dataRepository.saveAdwordsCampaign(account, id)
      case (Message(_, id, "campaigns", "deleted", _, _, _), account) =>
        dataRepository.deleteAdwordsCampaign(account, id)
      case (Message(_, id, "criterions", "downloaded", Some("KEYWORD"), Some(adGroupId), Some(campaignId)), account) =>
        dataRepository.saveAdwordsKeyword(account, campaignId, adGroupId, id)
      case (Message(_, id, "criterions", "deleted", Some("KEYWORD"), Some(adGroupId), _), account) =>
        dataRepository.deleteAdwordsKeyword(account, adGroupId, id)
      case (msg: Message, _) =>
        Future.failed(new RuntimeException("Не обработано: " + msg.toString))
    }
  }

  val ackFlow: Flow[FinalResult, Done, NotUsed] =
    Flow[FinalResult].mapAsyncUnordered(60) { result =>
      val msg = result._1
      result._2 match {
        case Right(_) =>
          msg.ack()
        case Left(e) =>
          logger.error(e.getMessage)
          msg.nack(requeue = false)
      }
    }

  val ackSink: Sink[Any, Future[Done]] = Sink.ignore
}
