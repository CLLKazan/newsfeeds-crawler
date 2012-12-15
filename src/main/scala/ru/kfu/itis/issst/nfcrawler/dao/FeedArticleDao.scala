/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.dao
import ru.kfu.itis.issst.nfcrawler.model.Article
import java.util.Date

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
trait FeedArticleDao {

  def getFeedId(feedUrl: String): Option[Int]

  def persistFeed(feedUrl: String): Int

  def getArticle(articleUrl: String): Option[Article]

  def persistArticle(article: Article)

  def updateFeedPubDate(pubDate: Date)
}