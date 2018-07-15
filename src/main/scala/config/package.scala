import com.typesafe.config.ConfigFactory

package object config {
  case class DatabaseConfig(driver: String, url: String, user: String, password: String)

  case class RabbitmqConfig(
    host: String,
    port: Short,
    username: String,
    password: String,
    exchange: String,
    queue: String,
    routingKey: String
  )

  case class Config(database: DatabaseConfig, rabbitmq: RabbitmqConfig)

  object Config {

    import pureconfig._

    def load(configFile: String = "application.conf"): Config = {
      loadConfig[Config](ConfigFactory.load(configFile)) match {
        case Right(config) => config
        case Left(e) => throw new RuntimeException(e.toString)
      }
    }
  }

}
