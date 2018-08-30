package app


import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import config.Config
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux
import stream.ImportFlow

object Application extends App with LazyLogging {
  val config = Config.load()
  logger.info("Starting")

  val metaTransactor: Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    config.metaDatabase.driver,
    config.metaDatabase.url,
    config.metaDatabase.user,
    config.metaDatabase.password
  )

  val dataTransactor: Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    config.dataDatabase.driver,
    config.dataDatabase.url,
    config.dataDatabase.user,
    config.dataDatabase.password
  )

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  ImportFlow.queueSource(config.rabbitmq).async
    .buffer(1000, OverflowStrategy.backpressure)
    .via(ImportFlow.decodeJson)
    .via(ImportFlow.findStatisticAccount)
    .via(ImportFlow.processSystem)
    .runWith(ImportFlow.ackSink)
}
