package ru.kfu.itis.issst.nfcrawler
import scala.actors.Actor
import grizzled.slf4j.Logging
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

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class FeedManager(feedUrl: URL, daoManager: DaoManager, httpManager: HttpManager,
  parsingManager: ParsingManager, extractionManager: ExtractionManager)
  extends Actor with Logging {

  private var feed: Feed = null
  private var parsedFeed: ParsedFeed = null
  private var parsedItemsMap = muta.Map.empty[URL, ParsedFeedItem]

  override val toString = "FeedManager[%s]".format(feedUrl)

  override def act() {
    List(daoManager, httpManager, parsingManager, extractionManager).foreach(link(_))
    daoManager ! new FeedRequest(feedUrl)
    loop {
      react {
        case msg @ FeedResponse(feed, request) =>
          debug(msg)
          this.feed = feed
          assert(feed.url == feedUrl && request.feedUrl == feedUrl)
          httpManager ! FeedContentRequest(feed.url)
        case msg @ FeedContentResponse(content, request) =>
          debug(msg)
          handleFeedContent(content)
        case msg @ FeedParsingResponse(parsedFeed, request) =>
          debug(msg)
          handleParsedFeed(parsedFeed)
        case msg @ ArticleResponse(articleOpt, request) =>
          debug(msg)
          handleArticle(articleOpt, request.url)
        case msg @ ArticlePageResponse(pageContent, request) =>
          debug(msg)
          handlePageContent(pageContent, request.articleUrl, request.articleId)
        case msg @ ExtractTextResponse(text, request) =>
          debug(msg)
          handleArticleText(text, request.url, request.articleId)
        case msg @ PersistArticleResponse(article, request) =>
          debug(msg)
          articlePersisted(article)
        case msg @ UpdateFeedResponse(request) =>
          debug(msg)
          feedUpdated(request.feed)
          finished()
      }
    }
  }

  private def handleFeedContent(content: String) {
    if (content == null) {
      error("Can't retrieve content of feed '%s'".format(feedUrl))
      exit()
    } else
      parsingManager ! FeedParsingRequest(content)
  }

  private def handleParsedFeed(parsedFeed: ParsedFeed) {
    if (parsedFeed == null) {
      error("Can't parse feed %s".format(feedUrl))
      exit()
    } else {
      this.parsedFeed = parsedFeed
      parsedFeed.items.foreach(item => { parsedItemsMap(item.url) = item })
      // check each entry
      for (item <- parsedFeed.items) {
        daoManager ! ArticleRequest(item.url)
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
        if (isArticleUpdated) info("Article '%s' pubDate has changed from %s to %s"
          .format(articleUrl, article.pubDate, pi.pubDate))
        (isArticleUpdated, Option(article.id))
      }
      case None => (true, Option.empty)
    }
    if (shouldUpdate) httpManager ! ArticlePageRequest(articleUrl, articleIdOpt)
    else itemProcessed(articleUrl)
  }

  private def handlePageContent(pageContent: String, url: URL, articleId: Option[Long]) {
    if (pageContent == null) {
      error("Can't retrieve content of page '%s'".format(url))
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
      error("Can't extract text from page '%s'".format(url))
      itemProcessed(url)
    }
  }

  private def articlePersisted(article: Article) {
    info("Article '%s' was persisted, text size : %s".format(article.url, article.text.length))
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
    info("All tasks for feed '%s' are performed. Last pub timestamp: %s"
      .format(feedUrl, parsedFeed.pubDate))
    // clean state
    parsedItemsMap = null
    parsedFeed = null
    exit()
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