/**
 *
 */
package ru.kfu.itis.issst.nfcrawler
import ru.kfu.itis.issst.nfcrawler.model.ParsedFeed
import java.net.URL
import ru.kfu.itis.issst.nfcrawler.model.Article
import java.util.Date

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
private[nfcrawler] object Messages {
  // dao
  case class FeedIdRequest(feedUrl: String)
  case class FeedIdResponse(id: Int, request: FeedIdRequest)
  case class ArticleRequest(url: URL)
  case class ArticleResponse(articleOpt: Option[Article], request: ArticleRequest)
  case class UpdatePubDateRequest(id: Int, pubDate: Date)
  case class UpdatePubDateResponse(request: UpdatePubDateRequest)
  // create OR update article depending on whether article.id is NULL
  case class PersistArticleRequest(article: Article)
  case class PersistArticleResponse(request: PersistArticleRequest)

  // http
  case class FeedContentRequest(feedUrl: String)
  case class FeedContentResponse(feedContent: String, request: FeedContentRequest)
  case class ArticlePageRequest(articleUrl: URL, articleId: Option[Int])
  case class ArticlePageResponse(pageContent: String, request: ArticlePageRequest)

  // parsing
  case class ContentParsingRequest(feedContent: String)
  case class ContentParsingResponse(parsedFeed: ParsedFeed, request: ContentParsingRequest)
  case class ExtractTextRequest(pageContent: String, url: URL, articleId: Option[Int])
  case class ExtractTextResponse(text: String, request: ExtractTextRequest)
}