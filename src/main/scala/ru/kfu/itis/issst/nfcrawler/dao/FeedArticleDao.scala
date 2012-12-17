/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.dao
import java.util.Date
import org.apache.commons.dbcp.BasicDataSource
import impl.MysqlFeedArticleDao
import ru.kfu.itis.issst.nfcrawler.util.SimpleFactory

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
trait FeedArticleDao {

  def getFeed(feedUrl: String): Option[Feed]

  protected def doPersistFeed(feed: Feed): Feed

  final def persistFeed(feed: Feed): Feed =
    if (feed.id != ID_NOT_PERSISTED)
      throw new IllegalArgumentException("Illegal feed entity id: %s".format(feed.id))
    else doPersistFeed(feed)

  def updateFeed(feed: Feed)

  def getArticle(articleUrl: String): Option[Article]

  protected def doPersistArticle(article: Article): Article

  final def persistArticle(article: Article): Article =
    if (article.id != ID_NOT_PERSISTED)
      throw new IllegalArgumentException("Illegal article entity id: %s".format(article.id))
    else doPersistArticle(article)

  def updateArticle(article: Article)
}

object FeedArticleDao extends SimpleFactory[DaoConfig, FeedArticleDao] {
  override protected def defaultBuilder(config: DaoConfig): FeedArticleDao = {
    val ds = new BasicDataSource
    ds.setDriverClassName(config.dbDriverClass)
    ds.setUrl(config.dbUrl)
    ds.setUsername(config.dbUserName)
    ds.setPassword(config.dbPassword)
    ds.setInitialSize(1)
    new MysqlFeedArticleDao(ds)
  }
}