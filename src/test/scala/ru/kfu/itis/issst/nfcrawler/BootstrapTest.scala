/**
 *
 */
package ru.kfu.itis.issst.nfcrawler
import org.scalatest.FunSuite
import dao.FeedArticleDao
import http.HttpFacade
import parser.FeedParser
import extraction.TextExtractor
import java.net.URL
import dao.Feed
import parser.ParsedFeed
import java.util.Date
import ru.kfu.itis.issst.nfcrawler.parser.ParsedFeedItem
import org.apache.commons.lang3.time.DateUtils
import dao.Article
import config.Configuration
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.hamcrest.Matcher
import org.mockito.Matchers._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.hamcrest.CustomTypeSafeMatcher
import scala.actors.Actor
import java.util.regex.Pattern

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class BootstrapTest extends FunSuite with MockitoSugar {

  test("Test workflow with mocked components") {
    val dao = mock[FeedArticleDao]
    val http = mock[HttpFacade]
    val parser = mock[FeedParser]
    val textExtractor = mock[TextExtractor]

    FeedArticleDao.setBuilder(cfg => dao)
    HttpFacade.setBuilder(cfg => http)
    FeedParser.setBuilder(cfg => parser)
    TextExtractor.setBuilder(cfg => textExtractor)

    // feed1 persisted
    val feedUrl1 = new URL("http://1.com/rss")
    val parsedFeed1 = new ParsedFeed(new Date(), 1.to(5).toList.map(
      i => new ParsedFeedItem(new URL("http://1.com/0" + i), new Date())))
    // feed2 new
    val feedUrl2 = new URL("http://2.com/rss")
    val parsedFeed2 = new ParsedFeed(new Date(), 1.to(5).toList.map(
      i => new ParsedFeedItem(new URL("http://2.com/0" + i), new Date())))

    when(dao.getFeed(feedUrl1.toString)).thenReturn(Some(new Feed(100, feedUrl1, null)))
    when(dao.getFeed(feedUrl2.toString)).thenReturn(None)
    when(dao.persistFeed(argThat(withUrl(feedUrl2))))
      .thenReturn(new Feed(200, feedUrl2, null))

    // articles
    when(dao.getArticle(contains("2.com"))).thenReturn(None)
    when(dao.getArticle(contains("1.com"))).thenReturn(None)
    when(dao.getArticle(endsWith("1.com/01"))).thenReturn(
      Some(new Article(10, new URL("http://1.com/01"),
        DateUtils.addYears(new Date(), -5), "some text", 100)))

    var articleIdCounter = 500L
    when(dao.persistArticle(any())).thenAnswer(answer(iom => {
      val srcArticle = iom.getArguments()(0).asInstanceOf[Article]
      articleIdCounter += 1
      new Article(articleIdCounter,
        srcArticle.url, srcArticle.pubDate, srcArticle.text, srcArticle.feedId)
    }))

    // http expectations
    when(http.getContent(argThat(urlEndsWith("rss")))).thenAnswer(answer(iom => {
      val url = iom.getArguments()(0).asInstanceOf[URL]
      url.toString + " feed content"
    }))
    when(http.getContent(argThat(urlMatches(Pattern.compile("\\d\\d$"))))).thenAnswer(answer(iom => {
      val url = iom.getArguments()(0).asInstanceOf[URL]
      url.toString + " article content"
    }))

    // feed parser expectations
    when(parser.parseFeed(startsWith(feedUrl1.toString())))
      .thenReturn(parsedFeed1)
    when(parser.parseFeed(startsWith(feedUrl2.toString())))
      .thenReturn(parsedFeed2)

    // extractor expectations
    when(textExtractor.extractFromHtml(anyString())).thenAnswer(answer(iom => {
      val src = iom.getArguments()(0).asInstanceOf[String]
      src + " w/o boilerplate"
    }))

    Bootstrap.start(makeConfig(feedUrl1, feedUrl2))

    // verify
    verify(dao, times(2)).updateFeed(any())
    verify(dao).updateArticle(any())
    assert(articleIdCounter === 509)
  }

  test("Test workflow with DAO errors") {
    val dao = mock[FeedArticleDao]
    val http = mock[HttpFacade]
    val parser = mock[FeedParser]
    val textExtractor = mock[TextExtractor]

    FeedArticleDao.setBuilder(cfg => dao)
    HttpFacade.setBuilder(cfg => http)
    FeedParser.setBuilder(cfg => parser)
    TextExtractor.setBuilder(cfg => textExtractor)

    val feedUrl1 = new URL("http://1.com/rss")
    val feedUrl2 = new URL("http://2.com/rss")

    doThrow(new RuntimeException).when(dao).getArticle(any())

    Bootstrap.start(makeConfig(feedUrl1, feedUrl2))
  }

  test("Test workflow with http errors") {
    val dao = mock[FeedArticleDao]
    val http = mock[HttpFacade]
    val parser = mock[FeedParser]
    val textExtractor = mock[TextExtractor]

    FeedArticleDao.setBuilder(cfg => dao)
    HttpFacade.setBuilder(cfg => http)
    FeedParser.setBuilder(cfg => parser)
    TextExtractor.setBuilder(cfg => textExtractor)

    val feedUrl1 = new URL("http://1.com/rss")
    val feedUrl2 = new URL("http://2.com/rss")

    when(dao.getFeed(feedUrl1.toString)).thenReturn(Some(new Feed(100, feedUrl1, null)))
    when(dao.getFeed(feedUrl2.toString)).thenReturn(Some(new Feed(100, feedUrl2, null)))

    doThrow(new RuntimeException).when(http).getContent(any())

    Bootstrap.start(makeConfig(feedUrl1, feedUrl2))
  }

  test("Test workflow with parser errors") {
    val dao = mock[FeedArticleDao]
    val http = mock[HttpFacade]
    val parser = mock[FeedParser]
    val textExtractor = mock[TextExtractor]

    FeedArticleDao.setBuilder(cfg => dao)
    HttpFacade.setBuilder(cfg => http)
    FeedParser.setBuilder(cfg => parser)
    TextExtractor.setBuilder(cfg => textExtractor)

    val feedUrl1 = new URL("http://1.com/rss")
    val feedUrl2 = new URL("http://2.com/rss")

    when(dao.getFeed(feedUrl1.toString)).thenReturn(Some(new Feed(100, feedUrl1, null)))
    when(dao.getFeed(feedUrl2.toString)).thenReturn(Some(new Feed(100, feedUrl2, null)))
    when(http.getContent(argThat(urlEndsWith("rss")))).thenAnswer(answer(iom => {
      val url = iom.getArguments()(0).asInstanceOf[URL]
      url.toString + " feed content"
    }))
    doThrow(new RuntimeException).when(parser).parseFeed(anyString())

    Bootstrap.start(makeConfig(feedUrl1, feedUrl2))
  }

  test("Test workflow with extractor errors") {
    val dao = mock[FeedArticleDao]
    val http = mock[HttpFacade]
    val parser = mock[FeedParser]
    val textExtractor = mock[TextExtractor]

    FeedArticleDao.setBuilder(cfg => dao)
    HttpFacade.setBuilder(cfg => http)
    FeedParser.setBuilder(cfg => parser)
    TextExtractor.setBuilder(cfg => textExtractor)

    val feedUrl1 = new URL("http://1.com/rss")
    val feedUrl2 = new URL("http://2.com/rss")

    when(dao.getFeed(feedUrl1.toString)).thenReturn(Some(new Feed(100, feedUrl1, null)))
    when(dao.getFeed(feedUrl2.toString)).thenReturn(Some(new Feed(100, feedUrl2, null)))
    when(dao.getArticle(anyObject())).thenReturn(None)
    when(http.getContent(argThat(urlEndsWith("rss")))).thenAnswer(answer(iom => {
      val url = iom.getArguments()(0).asInstanceOf[URL]
      url.toString + " feed content"
    }))
    when(http.getContent(argThat(urlMatches(Pattern.compile("\\d\\d$"))))).thenAnswer(answer(iom => {
      val url = iom.getArguments()(0).asInstanceOf[URL]
      url.toString + " article content"
    }))

    val parsedFeed1 = new ParsedFeed(new Date(), 1.to(5).toList.map(
      i => new ParsedFeedItem(new URL("http://1.com/0" + i), new Date())))
    val parsedFeed2 = new ParsedFeed(new Date(), 1.to(5).toList.map(
      i => new ParsedFeedItem(new URL("http://2.com/0" + i), new Date())))
    when(parser.parseFeed(startsWith(feedUrl1.toString())))
      .thenReturn(parsedFeed1)
    when(parser.parseFeed(startsWith(feedUrl2.toString())))
      .thenReturn(parsedFeed2)

    doThrow(new RuntimeException).when(textExtractor).extractFromHtml(anyString)
    Bootstrap.start(makeConfig(feedUrl1, feedUrl2))
  }

  private def makeConfig(urls: URL*) = new Configuration() {
    val feeds = Set(urls: _*)
    val hostAccessInterval = 1000
    val httpWorkersNumber = 3
    val dbDriverClass = ""
    val dbUserName = ""
    val dbPassword = ""
    val dbUrl = ""
  }

  private def withUrl(url: URL): Matcher[Feed] =
    new CustomTypeSafeMatcher[Feed]("feed with url " + url) {
      override protected def matchesSafely(feed: Feed): Boolean = feed.url == url
    }

  private def urlMatches(pattern: Pattern): Matcher[URL] =
    new CustomTypeSafeMatcher[URL]("pattern " + pattern + " matches") {
      override protected def matchesSafely(url: URL): Boolean =
        pattern.matcher(url.toString).find()
    }

  private def urlEndsWith(ending: String): Matcher[URL] =
    new CustomTypeSafeMatcher[URL]("ends with " + ending) {
      override protected def matchesSafely(url: URL): Boolean = url.toString.endsWith(ending)
    }

  private def answer[T](f: InvocationOnMock => T): Answer[T] = new Answer[T] {
    override def answer(iom: InvocationOnMock): T = f(iom)
  }
}