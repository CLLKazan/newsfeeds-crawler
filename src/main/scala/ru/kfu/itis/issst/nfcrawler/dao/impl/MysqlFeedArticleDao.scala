/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.dao.impl
import ru.kfu.itis.issst.nfcrawler.dao.FeedArticleDao
import ru.kfu.itis.issst.nfcrawler.dao.DaoConfig
import javax.sql.DataSource
import org.apache.commons.dbcp.BasicDataSource
import ru.kfu.itis.issst.nfcrawler.dao.{ FeedArticleDao, Feed, Article }
import java.sql.ResultSet
import java.net.URL
import java.io.StringReader

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
private[dao] class MysqlFeedArticleDao(val ds: DataSource) extends FeedArticleDao with JdbcTemplate {

  override def getFeed(feedUrl: String): Option[Feed] =
    getSingleResult[Feed]("SELECT id, url, last_pub_date FROM feed WHERE url = ?",
      _.setString(1, feedUrl))(rs =>
        new Feed(
          rs.getInt("id"),
          new URL(rs.getString("url")),
          rs.getDate("last_pub_date")
        )
      )

  override protected def doPersistFeed(feed: Feed): Feed = {
    val id =
      insertSingle("INSERT INTO feed(url, last_pub_date) VALUES (?,?)", _.getInt(1))(
        _.setString(1, feed.url.toString()),
        setDatetime(_, 2, feed.lastPubDate)
      )
    new Feed(id, feed.url, feed.lastPubDate)
  }

  override def updateFeed(feed: Feed) =
    update("UPDATE feed SET url = ?, last_pub_date = ? WHERE id = ?", 1)(
      _.setString(1, feed.url.toString),
      setDatetime(_, 2, feed.lastPubDate),
      _.setInt(3, feed.id)
    )

  override def getArticle(articleUrl: String): Option[Article] =
    getSingleResult[Article]("SELECT id, url, pub_date, txt, feed_id FROM article WHERE url = ? ",
      _.setString(1, articleUrl))(rs =>
        new Article(
          rs.getLong("id"),
          new URL(rs.getString("url")),
          rs.getDate("pub_date"),
          readClob(rs, "txt"),
          rs.getInt("feed_id")
        )
      )

  override protected def doPersistArticle(article: Article): Article = {
    val id = insertSingle("INSERT INTO article(url, pub_date, txt, feed_id) VALUES (?,?,?,?)", _.getLong(1))(
      _.setString(1, article.url.toString),
      setTimestamp(_, 2, article.pubDate),
      _.setClob(3, new StringReader(article.text), article.text.length),
      _.setInt(4, article.feedId)
    )
    new Article(id, article.url, article.pubDate, article.text, article.feedId)
  }

  override def updateArticle(article: Article) =
    update("UPDATE article SET url=?, pub_date=?, txt=? WHERE id = ?", 1)(
      _.setString(1, article.url.toString),
      setTimestamp(_, 2, article.pubDate),
      _.setClob(3, new StringReader(article.text), article.text.length),
      _.setLong(4, article.id)
    )

}