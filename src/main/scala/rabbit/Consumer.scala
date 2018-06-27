package rabbit

import app.Application.{executionContext, transactor}
import app.{Account, AdSystem}
import com.rabbitmq.client._
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import io.circe.parser._
import repository.AccountRepository

import scala.concurrent.Future
import scala.util.{Failure, Success}


class Consumer(channel: Channel) extends DefaultConsumer(channel) with LazyLogging {
  val accountRepository = new AccountRepository
  val direct = new Direct
  val adwords = new Adwords

  override def handleDelivery(
    consumerTag: String,
    envelope: Envelope,
    properties: AMQP.BasicProperties,
    body: Array[Byte]
  ): Unit = {
    import Consumer._

    Future {
      val messageString = new String(body, "UTF-8")

      val parseResult: Either[Exception, (Account, Message)] = for {
        message <- decode[Message](messageString)
        adSystem = AdSystem.withName(message.accountId.adSystem)
        accountId <- accountRepository.findAccountId(message.accountId.accountId, adSystem)
          .toRight(new RuntimeException("Аккаунт не найден в базе: " + message.accountId.accountId))
      } yield (Account(accountId, adSystem), message)

      val resultF = parseResult match {
        case Right((account@Account(_, AdSystem.Direct), message)) =>
          direct.process(account, message)
        case Right((account@Account(_, AdSystem.Adwords), message)) =>
          adwords.process(account, message)
        case Right(m) => Future {
          logger.error("Неизвестный тип аккаунта: " + m.toString())
        }
        case Left(e) => Future {
          logger.error(e.toString)
        }
      }

      resultF.onComplete {
        case Success(_) =>
          channel.basicAck(envelope.getDeliveryTag, false)
        case Failure(t) =>
          logger.error(t.toString)
          channel.basicReject(envelope.getDeliveryTag, false)
      }
    }
  }
}

object Consumer {

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

}

