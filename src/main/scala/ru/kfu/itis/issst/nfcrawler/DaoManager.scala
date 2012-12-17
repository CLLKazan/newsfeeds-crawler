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
import scala.actors.Exit
import util.actors.LogExceptionActor

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class DaoManager(daoConfig: DaoConfig) extends LogExceptionActor with Logging {
  private val dao = FeedArticleDao.get(daoConfig)
  this.trapExit = true

  override def act() {
    loop {
      react {
        case msg @ FeedRequest(feedUrl) =>
          debug(msg)
          sender ! FeedResponse(getFeed(feedUrl), msg)
        case msg @ ArticleRequest(articleUrl) =>
          debug(msg)
          sender ! ArticleResponse(getArticle(articleUrl), msg)
        case msg @ PersistArticleRequest(article) =>
          debug(msg)
          sender ! PersistArticleResponse(persistArticle(article), msg)
        case msg @ UpdateFeedRequest(feed) =>
          debug(msg)
          dao.updateFeed(feed)
          sender ! UpdateFeedResponse(msg)
        case msg @ Exit(from, Shutdown) =>
          info("Shutting down...")
          exit(Shutdown)
      }
    }
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