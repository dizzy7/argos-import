package app

import java.util.concurrent.Executors

import cats.effect.IO
import com.rabbitmq.client.{Channel, ConnectionFactory}
import com.typesafe.scalalogging.LazyLogging
import config.Config
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux
import rabbit.Consumer
import repository.{AccountRepository, DataRepository}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object Application extends App with LazyLogging {
  val config = Config.load()

  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1))
  implicit val transactor: Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    config.database.driver,
    config.database.url,
    config.database.user,
    config.database.password
  )

  val factory = new ConnectionFactory
  factory.setHost(config.rabbitmq.host)
  factory.setUsername(config.rabbitmq.username)
  factory.setPassword(config.rabbitmq.password)
  val exchangeName = config.rabbitmq.exchange
  val queueName = config.rabbitmq.queue
  val routingKey = config.rabbitmq.routingKey

  val connection = factory.newConnection()
  val channel: Channel = connection.createChannel()

  val accountRepository = new AccountRepository
  val dataRepository = new DataRepository

  //channel.exchangeDeclare(exchangeName, BuiltinExchangeType.FANOUT, true)
  //  channel.basicQos(100)
  channel.queueDeclare(queueName, true, false, true, null)
  channel.queueBind(queueName, exchangeName, routingKey)
  channel.basicConsume(queueName, false, new Consumer(channel))
}
