/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.dao.impl
import org.scalatest.FunSuite
import java.sql.DriverManager
import ru.kfu.itis.issst.nfcrawler.{ dao => daopkg }
import ru.kfu.itis.issst.nfcrawler.dao.{ DaoConfig, FeedArticleDao, Feed, Article }
import java.net.URL
import java.util.Date
import org.apache.commons.lang3.time.DateUtils
import java.sql.SQLException

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class MysqlFeedArticleDaoTest extends FunSuite {
  prepareDb()

  private val daoConfig = new DaoConfig() {
    val dbDriverClass = "org.hsqldb.jdbc.JDBCDriver"
    val dbUserName = "SA"
    val dbPassword = ""
    val dbUrl = "jdbc:hsqldb:mem:nfcrawler-test;ifexists=true;sql.syntax_mys=true"
  }
  private val dao = FeedArticleDao.build(daoConfig)

  private val url1 = new URL("http://example.com")
  private val url2 = new URL("http://dot.example.com")

  test("persist feed") {
    val feed1 = dao.persistFeed(new Feed(daopkg.ID_NOT_PERSISTED, url1, null))
    assert(feed1.id != daopkg.ID_NOT_PERSISTED)

    val feed2 = dao.persistFeed(new Feed(daopkg.ID_NOT_PERSISTED, url2, new Date()))
    assert(feed2.id != daopkg.ID_NOT_PERSISTED)
  }

  test("persist feed with assigned id") {
    intercept[IllegalArgumentException] {
      dao.persistFeed(new Feed(10, new URL("http://foobar.com/"), new Date()))
    }
  }

  test("get existent feed without date") {
    dao.getFeed(url1.toString) match {
      case Some(feed) => {
        assert(url1 === feed.url)
        assert(feed.id >= 0)
        assert(feed.lastPubDate === null)
      }
      case None => fail("can't get feed")
    }
  }

  test("get existent feed with date") {
    dao.getFeed(url2.toString) match {
      case Some(feed) => {
        assert(url2 === feed.url)
        assert(feed.id >= 0)
        assert(feed.lastPubDate != null)
      }
      case None => fail("can't get feed")
    }
  }

  test("get non-existent feed") {
    assert(!dao.getFeed("No such url").isDefined)
  }

  test("update feed") {
    val date = DateUtils.parseDate("2000.01.01", "yyyy.MM.dd")
    dao.updateFeed(new Feed(0, url1, date))
    val updated = dao.getFeed(url1.toString())
    assert(updated.isDefined)
    assert(updated.get.lastPubDate == date)
  }

  private val url3 = new URL("http://scala-lang.org")
  private val url4 = new URL("http://scala-ide.org")

  test("persist article without date") {
    val srcArticle = new Article(daopkg.ID_NOT_PERSISTED, url3, null, "some text", 0)
    val persistedArticle = dao.persistArticle(srcArticle)
    import persistedArticle._
    assert(id != daopkg.ID_NOT_PERSISTED)
    assert(url === srcArticle.url)
    assert(text === srcArticle.text)
    assert(pubDate === srcArticle.pubDate)
    assert(feedId === srcArticle.feedId)
  }

  test("persist article with date") {
    val article4 = new Article(daopkg.ID_NOT_PERSISTED, url4, new Date(), "some text", 1)
    val persistedArticle = dao.persistArticle(article4)
    import persistedArticle._
    assert(id != daopkg.ID_NOT_PERSISTED)
    assert(url === article4.url)
    assert(text === article4.text)
    assert(pubDate === article4.pubDate)
    assert(feedId === article4.feedId)
  }

  test("persist article with illegal feed id") {
    val srcArticle = new Article(daopkg.ID_NOT_PERSISTED, url3, null, "some text", 100)
    intercept[SQLException] {
      dao.persistArticle(srcArticle)
    }
  }

  test("persist article with assigned id") {
    val srcArticle = new Article(1, url3, null, "some text", 0)
    intercept[IllegalArgumentException] {
      dao.persistArticle(srcArticle)
    }
  }

  test("get and update article") {
    val dbArticleOpt = dao.getArticle(url4.toString())
    assert(dbArticleOpt.isDefined)
    val dbArticle = dbArticleOpt.get
    assert(dbArticle.id != 0)
    assert(dbArticle.url != null)
    assert(dbArticle.text != null)
    assert(dbArticle.pubDate != null)
    assert(dbArticle.feedId != 0)

    import dbArticle._
    val updatedText = "new text!"
    val updateArticle = new Article(id, url, pubDate, updatedText, feedId)
    dao.updateArticle(updateArticle)
    val dbArticle2 = dao.getArticle(url4.toString())
    assert(dbArticle2.get.text === updatedText)
  }

  test("get non-existent article") {
    assert(!dao.getArticle("fancy url").isDefined)
  }

  private def prepareDb() {
    Class.forName("org.hsqldb.jdbc.JDBCDriver")
    val con = DriverManager.getConnection(
      "jdbc:hsqldb:mem:nfcrawler-test;sql.syntax_mys=true",
      "SA", "")
    var stmt = con.prepareStatement("""
        CREATE TABLE feed (
    		id INT AUTO_INCREMENT PRIMARY KEY,
    		url VARCHAR(256) UNIQUE NOT NULL,
    		last_pub_date DATETIME)""")
    stmt.execute()
    stmt.close()
    stmt = con.prepareStatement("""CREATE TABLE article (
		id BIGINT AUTO_INCREMENT PRIMARY KEY,
		url VARCHAR(256) UNIQUE NOT NULL,
		pub_date TIMESTAMP,
		txt TEXT,
		feed_id INT NOT NULL,
		CONSTRAINT article_to_feed FOREIGN KEY (feed_id) REFERENCES feed(id)
    )""")
    stmt.execute()
    stmt.close()
    con.close()
  }
}