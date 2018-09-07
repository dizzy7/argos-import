package rabbit

import akka.NotUsed
import akka.stream.alpakka.amqp._
import akka.stream.alpakka.amqp.scaladsl.{AmqpSource, CommittableIncomingMessage}
import akka.stream.scaladsl.Source
import config.RabbitmqConfig

class Connection(config: RabbitmqConfig) {
  private val queueDeclaration = QueueDeclaration(config.queue)
    .withDurable(true)
    .withArguments(("x-message-ttl", 21600000.asInstanceOf[AnyRef]))
  private val bindingDeclaration = BindingDeclaration(config.queue, config.exchange)
  private val connectionProvider = AmqpDetailsConnectionProvider(config.host, config.port)
    .withAutomaticRecoveryEnabled(true)
    .withTopologyRecoveryEnabled(true)
    .withCredentials(AmqpCredentials(config.username, config.password))

  def createSource(): Source[CommittableIncomingMessage, NotUsed] =
    AmqpSource.committableSource(
      NamedQueueSourceSettings(connectionProvider, config.queue)
        .withDeclarations(queueDeclaration, bindingDeclaration),
      bufferSize = 1000
    )
}
