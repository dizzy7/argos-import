package app


import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import config.Config
import doobie.hikari.HikariTransactor
import stream.ImportFlow

object Application extends App with LazyLogging {
  val config = Config.load()
  logger.info("Starting")

  val metaTransactor: HikariTransactor[IO] = HikariTransactor.newHikariTransactor[IO](
    config.metaDatabase.driver,
    config.metaDatabase.url,
    config.metaDatabase.user,
    config.metaDatabase.password
  ).unsafeRunSync()

  val dataTransactor: HikariTransactor[IO] = HikariTransactor.newHikariTransactor[IO](
    config.dataDatabase.driver,
    config.dataDatabase.url,
    config.dataDatabase.user,
    config.dataDatabase.password
  ).unsafeRunSync()

  metaTransactor.configure({ f =>
    f.getHikariConfigMXBean.setMaximumPoolSize(30)
    IO.unit
  })
  dataTransactor.configure({ f =>
    f.getHikariConfigMXBean.setMaximumPoolSize(30)
    IO.unit
  })

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  ImportFlow.queueSource(config.rabbitmq).async
    .via(ImportFlow.decodeJson)
    .via(ImportFlow.findStatisticAccount)
    .via(ImportFlow.processSystem)
    .via(ImportFlow.ackFlow)
    .runWith(ImportFlow.ackSink)
}
