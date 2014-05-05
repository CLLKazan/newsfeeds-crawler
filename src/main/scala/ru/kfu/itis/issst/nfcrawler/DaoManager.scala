/**
 *
 */
package ru.kfu.itis.issst.nfcrawler
import dao.DaoConfig
import scala.collection.{ mutable => muta }
import Messages._
import java.net.URL
import dao.Article
import dao.Feed
import dao.FeedArticleDao
import ru.kfu.itis.issst.nfcrawler.{ dao => daopack }
import akka.actor.ActorLogging
import akka.actor.Actor

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class DaoManager(daoConfig: DaoConfig) extends Actor with ActorLogging {
  private val dao = FeedArticleDao.get(daoConfig)

  override val toString = "DaoManager"

  override def receive = {
    case msg @ FeedRequest(feedUrl) =>
      sender ! FeedResponse(getFeed(feedUrl), msg)
    case msg @ ArticleRequest(articleUrl) =>
      sender ! ArticleResponse(getArticle(articleUrl), msg)
    case msg @ PersistArticleRequest(article) =>
      sender ! PersistArticleResponse(persistArticle(article), msg)
    case msg @ UpdateFeedRequest(feed) =>
      dao.updateFeed(feed)
      sender ! UpdateFeedResponse(msg)
  }

  override def postStop() {
    log.info("Shutting down...")
  }

  private def getFeed(feedUrl: URL): Feed = dao.getFeed(feedUrl.toString()) match {
    case Some(feed) => feed
    case None => {
      // persist feed entity
      dao.persistFeed(Feed.build(feedUrl, null))
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