package ru.kfu.itis.issst.nfcrawler
import Messages._
import parser.ParsedFeedItem
import parser.ParsedFeed
import java.net.URL
import ru.kfu.itis.issst.nfcrawler.parser.ParsedFeed
import scala.collection.{ mutable => muta }
import dao.Article
import java.util.Date
import FeedManager._
import dao.Feed
import org.apache.commons.lang3.time.DateUtils
import java.util.Calendar
import akka.actor.ActorRef
import akka.event.Logging
import akka.actor.Actor
import akka.actor.ActorLogging
import scala.concurrent.duration._
import akka.actor.ReceiveTimeout
import akka.event.LoggingReceive

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class FeedManager(feedUrl: URL, daoManager: ActorRef, httpManager: ActorRef,
  parsingManager: ActorRef, extractionManager: ActorRef)
  extends Actor with ActorLogging {

  context.setReceiveTimeout(10 seconds)

  protected var feed: Feed = null
  protected var parsedFeed: ParsedFeed = null
  protected var parsedItemsMap = muta.Map.empty[URL, ParsedFeedItem]

  override val toString = "FeedManager[%s]".format(feedUrl)

  override def preStart() {
    log.debug("Starting")
  }

  override def receive() = LoggingReceive {
    case Initialize =>
      daoManager ! new FeedRequest(feedUrl)
    case FeedResponse(feed, request) =>
      this.feed = feed
      assert(feed.url == feedUrl && request.feedUrl == feedUrl)
      httpManager ! FeedContentRequest(feed.url)
    case FeedContentResponse(content, request) =>
      handleFeedContent(content)
    case FeedParsingResponse(parsedFeed, request) =>
      handleParsedFeed(parsedFeed)
    case ArticleResponse(articleOpt, request) =>
      handleArticle(articleOpt, request.url)
    case ArticlePageResponse(pageContent, request) =>
      handlePageContent(pageContent, request.articleUrl, request.articleId)
    case ExtractTextResponse(text, request) =>
      handleArticleText(text, request.url, request.articleId)
    case PersistArticleResponse(article, request) =>
      articlePersisted(article)
    case UpdateFeedResponse(request) =>
      feedUpdated(request.feed)
      finished()
    case ReceiveTimeout =>
      log.warning("Starving...")
  }

  private def handleFeedContent(content: String) {
    if (content == null) {
      log.error("Can't retrieve content of feed '{}'", feedUrl)
      context.stop(context.self)
    } else
      parsingManager ! FeedParsingRequest(content)
  }

  private def handleParsedFeed(parsedFeed: ParsedFeed) {
    if (parsedFeed == null) {
      // TODO persist this type of errors for analysis
      log.error("Can't parse feed {}", feedUrl)
      context.stop(context.self)
    } else {
      this.parsedFeed = parsedFeed
      if (parsedFeed.items == null || parsedFeed.items.isEmpty) {
        log.info("Parsed feed {} contains no items", feedUrl)
        finished()
      } else {
        parsedFeed.items.foreach(item => { parsedItemsMap(item.url) = item })
        // check each entry
        for (item <- parsedFeed.items) {
          daoManager ! ArticleRequest(item.url)
        }
      }
    }
  }

  private def handleArticle(articleOpt: Option[Article], articleUrl: URL) {
    val pi = parsedItemsMap.get(articleUrl) match {
      case Some(x) => x
      case None => unknownParsedUrl(articleUrl)
    }
    assert(pi.url == articleUrl)

    val (shouldUpdate, articleIdOpt) = articleOpt match {
      case Some(article) => {
        assert(article.url == articleUrl && article.feedId == this.feed.id)
        val isArticleUpdated = isNewer(pi.pubDate, article.pubDate)
        if (isArticleUpdated)
          log.info("Article '{}' pubDate has changed from {} to {}",
            articleUrl, article.pubDate, pi.pubDate)
        (isArticleUpdated, Option(article.id))
      }
      case None => (true, Option.empty)
    }
    if (shouldUpdate) httpManager ! ArticlePageRequest(articleUrl, articleIdOpt)
    else itemProcessed(articleUrl)
  }

  private def handlePageContent(pageContent: String, url: URL, articleId: Option[Long]) {
    if (pageContent == null) {
      log.error("Can't retrieve content of page '{}'", url)
      itemProcessed(url)
    } else
      extractionManager ! ExtractTextRequest(pageContent, url, articleId)
  }

  private def handleArticleText(text: String, url: URL, articleIdOpt: Option[Long]) {
    if (text != null) {
      val pi = parsedItemsMap.get(url) match {
        case Some(x) => x
        case None => unknownParsedUrl(url)
      }
      val articleId = articleIdOpt match {
        case Some(x) => x
        case None => dao.ID_NOT_PERSISTED
      }
      val updatedArticle = new Article(articleId, url, pi.pubDate, text, feed.id)
      daoManager ! PersistArticleRequest(updatedArticle)
    } else {
      log.error("Can't extract text from page '{}'", url)
      itemProcessed(url)
    }
  }

  private def articlePersisted(article: Article) {
    log.info("Article '{}' was persisted, text size : {}", article.url, article.text.length)
    itemProcessed(article.url)
  }

  private def itemProcessed(url: URL) {
    // remove from map
    parsedItemsMap.remove(url)
    // check whether it is last item
    if (parsedItemsMap.isEmpty) {
      // persist last pub date
      val updatedFeed = new Feed(feed.id, feed.url, parsedFeed.pubDate)
      daoManager ! UpdateFeedRequest(updatedFeed)
    }
  }

  private def feedUpdated(feed: Feed) {
    this.feed = feed
  }

  private def finished() {
    log.info("All tasks for feed '{}' are performed. Last pub timestamp: {}",
      feedUrl, parsedFeed.pubDate)
    // clean state
    parsedItemsMap = null
    parsedFeed = null
    context.stop(context.self)
  }

  private def unknownParsedUrl(url: URL): Nothing = throw new IllegalStateException(
    "Can't match url '%s' in map with keys:\n%s".format(
      url, parsedItemsMap.keySet))
}

object FeedManager {
  private def isNewer(target: Date, arg: Date): Boolean =
    if (target == null) false
    else if (arg == null) true
    else DateUtils.truncatedCompareTo(target, arg, Calendar.SECOND) > 0
}