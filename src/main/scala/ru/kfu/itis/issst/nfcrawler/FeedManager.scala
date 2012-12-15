package ru.kfu.itis.issst.nfcrawler
import scala.actors.Actor
import grizzled.slf4j.Logging
import ru.kfu.itis.issst.nfcrawler.Messages._
import ru.kfu.itis.issst.nfcrawler.model.ParsedFeedItem
import ru.kfu.itis.issst.nfcrawler.model.ParsedFeedItem
import java.net.URL
import ru.kfu.itis.issst.nfcrawler.model.ParsedFeed
import scala.collection.{ mutable => muta }
import ru.kfu.itis.issst.nfcrawler.model.Article
import java.util.Date
import FeedManager._

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class FeedManager(feedUrl: String,
  daoManager: DaoManager, httpManager: HttpManager, parsingManager: ParsingManager)
  extends Actor with Logging {

  var feedId: Int = -1
  var parsedFeed: ParsedFeed = null
  var parsedItemsMap = muta.Map.empty[URL, ParsedFeedItem]

  override def act() {
    daoManager ! new FeedIdRequest(feedUrl)
    loop {
      react {
        case FeedIdResponse(id, request) => {
          this.feedId = id
          assert(request.feedUrl == feedUrl)
          httpManager ! FeedContentRequest(feedUrl)
        }
        case FeedContentResponse(content, request) => {
          parsingManager ! ContentParsingRequest(content)
        }
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
        case PersistArticleResponse(request) => articlePersisted(request.article)
        case UpdatePubDateResponse(request) => finished()
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
        assert(article.url == articleUrl && article.feedId == this.feedId)
        (isNewer(pi.pubDate, article.pubDate), Option(article.id))
      }
      case None => (true, Option.empty)
    }
    if (shouldUpdate)
      httpManager ! ArticlePageRequest(articleUrl, articleIdOpt)
  }

  private def handlePageContent(pageContent: String, url: URL, articleId: Option[Int]) {
    parsingManager ! ExtractTextRequest(pageContent, url, articleId)
  }

  private def handleArticleText(text: String, url: URL, articleIdOpt: Option[Int]) {
    val pi = parsedItemsMap.get(url) match {
      case Some(x) => x
      case None => unknownParsedUrl(url)
    }
    val articleId = articleIdOpt match {
      case Some(x) => x
      case None => Article.ID_NOT_PERSISTED
    }
    val updatedArticle = new Article(articleId, url, pi.pubDate, text, feedId)
    daoManager ! PersistArticleRequest(updatedArticle)
  }

  private def articlePersisted(article: Article) {
    // remove from map
    parsedItemsMap.remove(article.url)
    // check whether it is last item
    if (parsedItemsMap.isEmpty) {
      // persist last pub date
      daoManager ! UpdatePubDateRequest(feedId, parsedFeed.pubDate)
    }
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