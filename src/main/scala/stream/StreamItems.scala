package stream

import akka.{Done, NotUsed}
import akka.stream.alpakka.amqp.scaladsl.CommittableIncomingMessage
import akka.stream.scaladsl._
import app.{ContextSystem, StatisticAccount}
import rabbit.{Connection, Consumer}
import config.RabbitmqConfig
import io.circe.parser.decode
import rabbit.Consumer.{AccountInfo, Message}
import repository.{AccountRepository, DataRepository}
import app.Application.transactor
import app.ContextSystem.{Adwords, Direct}
import io.circe.generic.auto._
import io.circe.parser._
import app.Application.executionContext

import scala.concurrent.Future

object StreamItems {

  sealed trait MessageAction

  object Ack extends MessageAction

  object Reject extends MessageAction

  object Requeue extends MessageAction

  val accountRepository = new AccountRepository()
  val dataRepository = new DataRepository()

  def queueSource(config: RabbitmqConfig): Source[CommittableIncomingMessage, NotUsed] = {
    val connection = new Connection(config)
    connection.createSource()
  }

  val decodeJson: Flow[CommittableIncomingMessage, Message, NotUsed] =
    Flow[CommittableIncomingMessage].map[Message]({ m =>
      val messageString = m.message.bytes.decodeString("UTF-8")
      decode[Message](messageString) match {
        case Right(message) => message
        case Left(e) => throw e
      }
    })

  val getAccount: Flow[Message, (Message, StatisticAccount), NotUsed] =
    Flow[Message].map[(Message, StatisticAccount)]({ message =>
      val accountInfo = for {
        adSystem <- ContextSystem(message.accountId.adSystem)
        accountId <- accountRepository.findAccountId(message.accountId.accountId, adSystem)
      } yield (adSystem, accountId)

      accountInfo match {
        case Some((adSystem, accountId)) => (message, StatisticAccount(accountId, adSystem))
        case None => throw new RuntimeException("Аккаунт не найден")
      }
    })

  def filterSystem(system: ContextSystem): Flow[(Message, StatisticAccount), (Message, StatisticAccount), NotUsed] =
    Flow[(Message, StatisticAccount)].filter(in => in._2.adSystem == system)

  def systemProcess(contextSystem: ContextSystem): Flow[(Message, StatisticAccount), Unit, NotUsed] = {
    contextSystem match {
      case Direct => directProcess
      case Adwords => adwordsProcess
      case _ => throw new RuntimeException("Нет обработчика для системы" + contextSystem.toString)
    }
  }

  val directProcess: Flow[(Message, StatisticAccount), Unit, NotUsed] =
    Flow[(Message, StatisticAccount)].mapAsync(4) {
      case (Message(_, id, "campaigns", "downloaded", _, _, _), account) =>
        dataRepository.saveDirectCampaign(account, id)
      case (Message(_, id, "campaigns", "deleted", _, _, _), account) =>
        dataRepository.deleteDirectCampaign(account, id)
      case (Message(_, id, "keywords", "downloaded", _, Some(adGroupId), Some(campaignId)), account) =>
        dataRepository.saveDirectKeyword(account, campaignId, adGroupId, id)
      case (Message(_, id, "keywords", "deleted", _, _, _), account) =>
        dataRepository.deleteDirectKeyword(account, id)
      case (msg, _) => Future {
        throw new RuntimeException("Не обработано: " + msg.toString)
      }
    }.recover {
      case _ => println("Direct save false")
    }

  val adwordsProcess: Flow[(Message, StatisticAccount), Unit, NotUsed] =
    Flow[(Message, StatisticAccount)].mapAsync(4) {
      case (Message(_, id, "campaigns", "downloaded", _, _, _), account) =>
        dataRepository.saveAdwordsCampaign(account, id)
      case (Message(_, id, "campaigns", "deleted", _, _, _), account) =>
        dataRepository.deleteAdwordsCampaign(account, id)
      case (Message(_, id, "criterions", "downloaded", Some("KEYWORD"), Some(adGroupId), Some(campaignId)), account) =>
        dataRepository.saveAdwordsKeyword(account, campaignId, adGroupId, id)
      case (Message(_, id, "criterions", "deleted", Some("KEYWORD"), Some(adGroupId), _), account) =>
        dataRepository.deleteAdwordsKeyword(account, adGroupId, id)
      case (msg, _) => Future {
        throw new RuntimeException("Не обработано: " + msg.toString)
      }
    }.recover {
      case _ => println("Adwords save false")
    }

  val ackSink: Sink[(Unit, CommittableIncomingMessage), Future[Done]] =
    Sink.foreach[(Unit, CommittableIncomingMessage)] {
      case (_, msg) =>
        msg.ack()
    }
}
