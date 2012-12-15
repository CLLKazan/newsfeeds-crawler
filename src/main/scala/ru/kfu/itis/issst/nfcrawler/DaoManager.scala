/**
 *
 */
package ru.kfu.itis.issst.nfcrawler
import ru.kfu.itis.issst.nfcrawler.dao.DaoConfig
import scala.actors.Actor
import grizzled.slf4j.Logging
import scala.collection.{ mutable => muta }
import Messages._
import dao.impl.MysqlFeedArticleDao
import java.net.URL
import model.Article

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class DaoManager(daoConfig: DaoConfig) extends Actor with Logging {
  val dao = MysqlFeedArticleDao.build(daoConfig)

  override def act() {
    loop {
      react {
        case msg @ FeedIdRequest(feedUrl) =>
          sender ! FeedIdResponse(getFeedId(feedUrl), msg)
        case msg @ ArticleRequest(articleUrl) =>
          sender ! ArticleResponse(getArticle(articleUrl), msg)
        case msg @ PersistArticleRequest(article) => {
          persistArticle(article)
          sender ! PersistArticleResponse(msg)
        }
      }
    }
  }

  private def getFeedId(feedUrl: String): Int = {
    val feedId = dao.getFeedId(feedUrl) match {
      case Some(x) => x
      case None => {
        // persist feed entity
        dao.persistFeed(feedUrl)
      }
    }
    feedId
  }

  private def getArticle(articleUrl: URL): Option[Article] = {
    dao.getArticle(articleUrl.toString())
  }
  
  private def persistArticle(article:Article){
    // TODO XXX XXX
  }
}