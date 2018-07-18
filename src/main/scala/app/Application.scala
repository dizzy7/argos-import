package app

import java.util.concurrent.Executors

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import config.Config
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux
import stream.ImportFlow

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
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  ImportFlow.queueSource(config.rabbitmq)
    .via(ImportFlow.decodeJson)
    .via(ImportFlow.findStatisticAccount)
    .via(ImportFlow.processSystem)
    .runWith(ImportFlow.ackSink)
}
