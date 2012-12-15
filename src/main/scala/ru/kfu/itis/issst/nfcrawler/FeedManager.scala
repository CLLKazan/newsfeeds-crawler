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

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class FeedManager(feedUrl: String,
  daoManager: DaoManager, httpManager: HttpManager, parsingManager: ParsingManager)
  extends Actor with Logging {

  private var feed: Feed = null
  private var parsedFeed: ParsedFeed = null
  private var parsedItemsMap = muta.Map.empty[URL, ParsedFeedItem]

  override def act() {
    daoManager ! new FeedRequest(feedUrl)
    loop {
      react {
        case FeedResponse(feed, request) => {
          this.feed = feed
          assert(feed.url == feedUrl && request.feedUrl == feedUrl)
          httpManager ! FeedContentRequest(feed.url)
        }
        case FeedContentResponse(content, request) => handleFeedContent(content)
        case ContentParsingResponse(parsedFeed, request) => {
          this.parsedFeed = parsedFeed
          parsedFeed.items.foreach(item => { parsedItemsMap(item.url) = item })
          // check each entry
          for (item <- parsedFeed.items) {
            daoManager ! ArticleRequest(item.url)
          }

        }
        case ArticleResponse(articleOpt, request) => handleArticle(articleOpt, request.url)
        case ArticlePageResponse(pageContent, request) => handlePageContent(pageContent, request.articleUrl, request.articleId)
        case ExtractTextResponse(text, request) => handleArticleText(text, request.url, request.articleId)
        case PersistArticleResponse(article, request) => articlePersisted(article)
        case UpdateFeedResponse(request) => {
          feedUpdated(request.feed)
          finished()
        }
      }
    }
  }

  private def handleFeedContent(content: String) {
    if (content == null) {
      error("Can't retrieve content of feed '%s'".format(feedUrl))
      exit()
    } else
      parsingManager ! ContentParsingRequest(content)
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
        (isNewer(pi.pubDate, article.pubDate), Option(article.id))
      }
      case None => (true, Option.empty)
    }
    if (shouldUpdate)
      httpManager ! ArticlePageRequest(articleUrl, articleIdOpt)
  }

  private def handlePageContent(pageContent: String, url: URL, articleId: Option[Int]) {
    if (pageContent == null) {
      error("Can't retrieve content of page '%s'".format(url))
      itemProcessed(url)
    } else
      parsingManager ! ExtractTextRequest(pageContent, url, articleId)
  }

  private def handleArticleText(text: String, url: URL, articleIdOpt: Option[Int]) {
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
    info("All tasks for feed '%s' are performed. Last pub timestamp: %2tY.%2tm.%2td %2tH:%2tM:%2tS:%2tL"
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
    else target.compareTo(arg) > 0
}