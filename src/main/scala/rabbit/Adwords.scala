package rabbit

import app.Account
import app.Application.{executionContext, transactor}
import com.typesafe.scalalogging.LazyLogging
import rabbit.Consumer.Message
import repository.DataRepository

import scala.concurrent.Future

class Adwords extends LazyLogging {

  val dataRepository = new DataRepository()

  def process(account: Account, message: Consumer.Message): Future[Unit] = {
    message match {
      case Message(_, id, "campaigns", "downloaded", _, _, _) =>
        dataRepository.saveAdwordsCampaign(account, id)
      case Message(_, id, "campaigns", "deleted", _, _, _) =>
        dataRepository.deleteAdwordsCampaign(account, id)
      case Message(_, id, "criterions", "downloaded", Some("KEYWORD"), Some(adGroupId), Some(campaignId)) =>
        dataRepository.saveAdwordsKeyword(account, campaignId, adGroupId, id)
      case Message(_, id, "criterions", "deleted", Some("KEYWORD"), Some(adGroupId), _) =>
        dataRepository.deleteAdwordsKeyword(account, adGroupId, id)
      case msg: Message => Future {
        logger.info("Не обработано: " + msg.toString)
      }
    }
  }
}

