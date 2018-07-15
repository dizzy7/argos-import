package rabbit

import akka.{Done, NotUsed}
import akka.stream.alpakka.amqp._
import akka.stream.alpakka.amqp.scaladsl.{AmqpSink, AmqpSource, CommittableIncomingMessage}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import config.RabbitmqConfig

import scala.concurrent.Future

class Connection(config: RabbitmqConfig) {
  private val queueDeclaration = QueueDeclaration(config.queue).withAutoDelete(true)
  private val bindingDeclaration = BindingDeclaration(config.queue, config.exchange)
  private val connectionProvider = AmqpDetailsConnectionProvider(config.host, config.port)
    .withAutomaticRecoveryEnabled(true)
    .withTopologyRecoveryEnabled(true)
    .withCredentials(AmqpCredentials(config.username, config.password))

  def createSource(): Source[CommittableIncomingMessage, NotUsed] =
    AmqpSource.committableSource(
      NamedQueueSourceSettings(connectionProvider, config.queue)
        .withDeclarations(queueDeclaration, bindingDeclaration),
      bufferSize = 100
    )
}
