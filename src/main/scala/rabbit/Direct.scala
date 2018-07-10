package rabbit

import app.StatisticAccount
import app.Application.{executionContext, transactor}
import com.typesafe.scalalogging.LazyLogging
import rabbit.Consumer.Message
import repository.DataRepository

import scala.concurrent.Future

class Direct extends LazyLogging {
  val dataRepository = new DataRepository()

  def process(account: StatisticAccount, message: Consumer.Message): Future[Unit] = {
    message match {
      case Message(_, id, "campaigns", "downloaded", _, _, _) =>
        dataRepository.saveDirectCampaign(account, id)
      case Message(_, id, "campaigns", "deleted", _, _, _) =>
        dataRepository.deleteDirectCampaign(account, id)
      case Message(_, id, "keywords", "downloaded", _, Some(adGroupId), Some(campaignId)) =>
        dataRepository.saveDirectKeyword(account, campaignId, adGroupId, id)
      case Message(_, id, "keywords", "deleted", _, _, _) =>
        dataRepository.deleteDirectKeyword(account, id)
      case msg: Message => Future {
        logger.info("Не обработано: " + msg.toString)
      }
    }
  }
}
