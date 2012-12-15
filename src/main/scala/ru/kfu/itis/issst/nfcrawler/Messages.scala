/**
 *
 */
package ru.kfu.itis.issst.nfcrawler
import parser.ParsedFeed
import java.net.URL
import dao.Article
import java.util.Date
import dao.Feed

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
private[nfcrawler] object Messages {
  // dao
  case class FeedRequest(feedUrl: String)
  case class FeedResponse(feed: Feed, request: FeedRequest)
  case class ArticleRequest(url: URL)
  case class ArticleResponse(articleOpt: Option[Article], request: ArticleRequest)
  case class UpdateFeedRequest(feed: Feed)
  case class UpdateFeedResponse(request: UpdateFeedRequest)
  // create OR update article depending on whether article.id is NULL
  case class PersistArticleRequest(article: Article)
  case class PersistArticleResponse(article: Article, request: PersistArticleRequest)

  // http
  case class FeedContentRequest(feedUrl: URL)
  case class FeedContentResponse(feedContent: String, request: FeedContentRequest)
  case class ArticlePageRequest(articleUrl: URL, articleId: Option[Int])
  case class ArticlePageResponse(pageContent: String, request: ArticlePageRequest)

  // parsing
  case class ContentParsingRequest(feedContent: String)
  case class ContentParsingResponse(parsedFeed: ParsedFeed, request: ContentParsingRequest)
  case class ExtractTextRequest(pageContent: String, url: URL, articleId: Option[Int])
  case class ExtractTextResponse(text: String, request: ExtractTextRequest)
}