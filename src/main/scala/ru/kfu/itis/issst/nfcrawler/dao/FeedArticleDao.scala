/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.dao
import java.util.Date

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
trait FeedArticleDao {

  def getFeed(feedUrl: String): Option[Feed]

  def persistFeed(feed: Feed): Feed

  def updateFeed(feed: Feed)

  def getArticle(articleUrl: String): Option[Article]

  def persistArticle(article: Article): Article

  def updateArticle(article: Article)
}