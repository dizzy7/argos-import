package app

import java.util.concurrent.Executors

import akka.NotUsed
import akka.actor.ActorSystem
import akka.io.Udp.SO
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.alpakka.amqp.scaladsl.CommittableIncomingMessage
import akka.stream.scaladsl._
import app.ContextSystem.Direct
import cats.effect.IO
import com.rabbitmq.client.{Channel, ConnectionFactory}
import com.typesafe.scalalogging.LazyLogging
import config.Config
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux
import rabbit.Consumer.Message
import rabbit.{Connection, Consumer}
import repository.{AccountRepository, DataRepository}
import stream.StreamItems
import stream.StreamItems.MessageAction

import scala.concurrent.duration._
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

  val graph = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
    import GraphDSL.Implicits._

    val mainBroadcast = b.add(Broadcast[CommittableIncomingMessage](2))
    val finalZip = b.add(Zip[Unit, CommittableIncomingMessage]())
    val queueSource = StreamItems.queueSource(config.rabbitmq)

    val systemsFlow = b.add(Broadcast[(Message, StatisticAccount)](ContextSystem.systems.length))
    val systemsMerge = b.add(Merge[Unit](ContextSystem.systems.length))

    ContextSystem.systems.zipWithIndex.foreach {
      case (adSystem, i) =>
        systemsFlow.out(i) ~>
          StreamItems.filterSystem(adSystem) ~>
          StreamItems.systemProcess(adSystem) ~>
          systemsMerge.in(i)
    }

    queueSource ~> mainBroadcast.in

    mainBroadcast.out(0) ~> StreamItems.decodeJson ~> StreamItems.getAccount ~> systemsFlow
    systemsMerge ~> finalZip.in0
    mainBroadcast.out(1) ~> finalZip.in1

    finalZip.out ~> StreamItems.ackSink

    ClosedShape
  }).run()
}
