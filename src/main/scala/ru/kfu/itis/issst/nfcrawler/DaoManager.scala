/**
 *
 */
package ru.kfu.itis.issst.nfcrawler
import dao.DaoConfig
import scala.actors.Actor
import grizzled.slf4j.Logging
import scala.collection.{ mutable => muta }
import Messages._
import java.net.URL
import dao.Article
import dao.Feed
import dao.FeedArticleDao
import ru.kfu.itis.issst.nfcrawler.{ dao => daopack }

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class DaoManager(daoConfig: DaoConfig) extends Actor with Logging {
  private val dao = FeedArticleDao.build(daoConfig)

  override def act() {
    loop {
      react {
        case msg @ FeedRequest(feedUrl) =>
          sender ! FeedResponse(getFeed(feedUrl), msg)
        case msg @ ArticleRequest(articleUrl) =>
          sender ! ArticleResponse(getArticle(articleUrl), msg)
        case msg @ PersistArticleRequest(article) =>
          sender ! PersistArticleResponse(persistArticle(article), msg)
        case msg @ UpdateFeedRequest(feed) => {
          dao.updateFeed(feed)
          sender ! UpdateFeedResponse(msg)
        }
        case Stop => exit()
      }
    }
  }

  private def getFeed(feedUrl: String): Feed = dao.getFeed(feedUrl) match {
    case Some(feed) => feed
    case None => {
      // persist feed entity
      dao.persistFeed(Feed.build(new URL(feedUrl), null))
    }
  }

  private def getArticle(articleUrl: URL): Option[Article] =
    dao.getArticle(articleUrl.toString())

  private def persistArticle(article: Article): Article = article.id match {
    case daopack.ID_NOT_PERSISTED => dao.persistArticle(article)
    case articleId => {
      dao.updateArticle(article)
      article
    }
  }
}