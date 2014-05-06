/**
 *
 */
package ru.kfu.itis.issst.nfcrawler

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.FlatSpecLike
import org.scalatest.fixture.FlatSpec
import com.typesafe.config.ConfigFactory
import akka.testkit.TestProbe
import java.net.URL
import akka.actor.Props
import scala.concurrent.duration._
import ru.kfu.itis.issst.nfcrawler.dao._
import ru.kfu.itis.issst.nfcrawler.parser._
import org.apache.commons.lang3.time.DateUtils
import java.util.Date

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class FeedManagerTest extends FlatSpec {

  import FeedManagerTest._
  import Messages._

  behavior of "FeedManager"

  it should "not start working until Initialize is sent" in { f =>
    pending
  }

  it should "tell DaoManager after own initialization" in { f =>
    implicit val actSys = f.actSys
    val daoProbe = TestProbe()
    val feedManager = actSys.actorOf(Props(
      new FeedManager(TestFeedUrl, daoProbe.ref, TestProbe().ref, TestProbe().ref, TestProbe().ref)))
    feedManager ! Initialize
    daoProbe.expectMsg(FeedRequest(TestFeedUrl))
  }

  it should "tell HttpManager to download a feed content" in { f =>
    implicit val actSys = f.actSys
    val httpProbe = TestProbe()
    val feedManager = actSys.actorOf(Props(
      new FeedManager(TestFeedUrl, TestProbe().ref, httpProbe.ref, TestProbe().ref, TestProbe().ref)))
    feedManager ! FeedResponse(new Feed(1, TestFeedUrl, null), FeedRequest(TestFeedUrl))
    httpProbe.expectMsg(FeedContentRequest(TestFeedUrl))
  }

  it should "tell ParsingManager to parse a feed content" in { f =>
    implicit val actSys = f.actSys
    val parsingProbe = TestProbe()
    val feedManager = actSys.actorOf(Props(
      new FeedManager(TestFeedUrl, TestProbe().ref, TestProbe().ref, parsingProbe.ref, TestProbe().ref)))
    feedManager ! FeedContentResponse("some-feed-xml", FeedContentRequest(TestFeedUrl))
    parsingProbe.expectMsg(FeedParsingRequest("some-feed-xml"))
  }

  it should "stop if parsing of a feed content is failed" in { f =>
    implicit val actSys = f.actSys
    val observer = TestProbe()
    val feedManager = actSys.actorOf(Props(
      new FeedManager(TestFeedUrl, TestProbe().ref, TestProbe().ref, TestProbe().ref, TestProbe().ref)))
    observer.watch(feedManager)
    feedManager ! FeedParsingResponse(null, FeedParsingRequest("some-feed-xml"))
    observer.expectTerminated(feedManager, 10 seconds)
  }

  it should "stop if feed content does not contain items" in { f =>
    implicit val actSys = f.actSys
    val observer = TestProbe()
    val feedManager = actSys.actorOf(Props(
      new FeedManager(TestFeedUrl, TestProbe().ref, TestProbe().ref, TestProbe().ref, TestProbe().ref)))
    observer.watch(feedManager)
    feedManager ! FeedParsingResponse(
      new ParsedFeed(null, Nil),
      FeedParsingRequest("some-feed-xml"))
    observer.expectTerminated(feedManager, 10 seconds)
  }

  it should "check each parsed item in DAO" in { f =>
    implicit val actSys = f.actSys
    val daoProbe = TestProbe()
    val feedManager = actSys.actorOf(Props(
      new FeedManager(TestFeedUrl, daoProbe.ref, TestProbe().ref, TestProbe().ref, TestProbe().ref)))
    val parsedFeed = new ParsedFeed(null,
      new ParsedFeedItem("http://example.com/items/1", null) ::
        new ParsedFeedItem("http://example.com/items/2", null) :: Nil)
    feedManager ! FeedParsingResponse(parsedFeed, FeedParsingRequest("some-feed-xml"))
    daoProbe.expectMsgAllOf(
      ArticleRequest("http://example.com/items/1"),
      ArticleRequest("http://example.com/items/2"))
  }

  it should "download content of a new article" in { f =>
    implicit val actSys = f.actSys
    val httpProbe = TestProbe()
    val feedManager = actSys.actorOf(Props(
      new FeedManager(TestFeedUrl, TestProbe().ref, httpProbe.ref, TestProbe().ref, TestProbe().ref) {
        parsedItemsMap("http://example.com/items/3") =
          new ParsedFeedItem("http://example.com/items/3", null)
      }))
    feedManager ! ArticleResponse(None, ArticleRequest("http://example.com/items/3"))
    httpProbe.expectMsg(
      ArticlePageRequest("http://example.com/items/3", None))
  }

  it should "download content of an updated article" in { f =>
    implicit val actSys = f.actSys
    val httpProbe = TestProbe()
    val feedManager = actSys.actorOf(Props(
      new FeedManager(TestFeedUrl, TestProbe().ref, httpProbe.ref, TestProbe().ref, TestProbe().ref) {
        parsedItemsMap("http://example.com/items/3") =
          new ParsedFeedItem("http://example.com/items/3", today)
        feed = new Feed(100, "http://example.com/rss", null)
      }))
    feedManager ! ArticleResponse(
      Some(new Article(3, "http://example.com/items/3", dayBefore, "some text", 100)),
      ArticleRequest("http://example.com/items/3"))
    httpProbe.expectMsg(
      ArticlePageRequest("http://example.com/items/3", Some(3)))
  }

  it should "download content of an updated article if the previous timestamp is null)" in { f =>
    implicit val actSys = f.actSys
    val httpProbe = TestProbe()
    val feedManager = actSys.actorOf(Props(
      new FeedManager(TestFeedUrl, TestProbe().ref, httpProbe.ref, TestProbe().ref, TestProbe().ref) {
        parsedItemsMap("http://example.com/items/3") =
          new ParsedFeedItem("http://example.com/items/3", today)
        feed = new Feed(100, "http://example.com/rss", null)
      }))
    feedManager ! ArticleResponse(
      Some(new Article(3, "http://example.com/items/3", null, "some text", 100)),
      ArticleRequest("http://example.com/items/3"))
    httpProbe.expectMsg(
      ArticlePageRequest("http://example.com/items/3", Some(3)))
  }

  it should "ignore an article with the same timestamp" in { f =>
    implicit val actSys = f.actSys
    val httpProbe = TestProbe()
    val feedManager = actSys.actorOf(Props(
      new FeedManager(TestFeedUrl, TestProbe().ref, httpProbe.ref, TestProbe().ref, TestProbe().ref) {
        parsedItemsMap("http://example.com/items/3") =
          new ParsedFeedItem("http://example.com/items/3", dayBefore)
        // add one more to avoid further step
        parsedItemsMap("http://exampl.com/items/4") =
          new ParsedFeedItem("http://example.com/items/4", today)
        feed = new Feed(100, "http://example.com/rss", null)
      }))
    feedManager ! ArticleResponse(
      Some(new Article(3, "http://example.com/items/3", dayBefore, "some text", 100)),
      ArticleRequest("http://example.com/items/3"))
    httpProbe.expectNoMsg(5 seconds)
  }

  it should "extract text from an article page" in { f =>
    implicit val actSys = f.actSys
    val extractorProbe = TestProbe()
    val feedManager = actSys.actorOf(Props(
      new FeedManager(TestFeedUrl, TestProbe().ref, TestProbe().ref, TestProbe().ref, extractorProbe.ref)))
    feedManager ! ArticlePageResponse("some-page-html",
      new ArticlePageRequest("http://example.com/items/5", Some(5)))
    extractorProbe.expectMsg(
      ExtractTextRequest("some-page-html", "http://example.com/items/5", Some(5)))
  }

  it should "avoid null content from an article URL" in { f =>
    implicit val actSys = f.actSys
    val extractorProbe = TestProbe()
    val feedManager = actSys.actorOf(Props(
      new FeedManager(TestFeedUrl, TestProbe().ref, TestProbe().ref, TestProbe().ref, extractorProbe.ref) {
        parsedItemsMap("http://example.com/items/5") = new ParsedFeedItem("http://example.com/items/5", null)
        parsedItemsMap("http://example.com/items/6") = new ParsedFeedItem("http://example.com/items/6", null)
      }))
    feedManager ! ArticlePageResponse(null,
      new ArticlePageRequest("http://example.com/items/5", Some(5)))
    extractorProbe.expectNoMsg(5 seconds)
  }

  it should "persist extracted text for unseen article" in { f =>
    implicit val actSys = f.actSys
    val daoProbe = TestProbe()
    val feedManager = actSys.actorOf(Props(
      new FeedManager(TestFeedUrl, daoProbe.ref, TestProbe().ref, TestProbe().ref, TestProbe().ref) {
        parsedItemsMap("http://example.com/items/6") = new ParsedFeedItem("http://example.com/items/6", dayBefore)
        feed = new Feed(100, "http://example.com", null)
      }))
    val request = ExtractTextRequest("some-page-html", "http://example.com/items/6", None)
    feedManager ! ExtractTextResponse("some-text-w/o-html", request)
    daoProbe.expectMsg(PersistArticleRequest(new Article(
      ID_NOT_PERSISTED, "http://example.com/items/6", dayBefore, "some-text-w/o-html", 100)))
  }

  it should "persist extracted text for updated article" in { f =>
    implicit val actSys = f.actSys
    val daoProbe = TestProbe()
    val feedManager = actSys.actorOf(Props(
      new FeedManager(TestFeedUrl, daoProbe.ref, TestProbe().ref, TestProbe().ref, TestProbe().ref) {
        parsedItemsMap("http://example.com/items/7") = new ParsedFeedItem("http://example.com/items/7", dayBefore)
        feed = new Feed(200, "http://example.com", null)
      }))
    val request = ExtractTextRequest("some-page-html", "http://example.com/items/7", Some(7))
    feedManager ! ExtractTextResponse("some-text-w/o-html", request)
    daoProbe.expectMsg(PersistArticleRequest(new Article(
      7, "http://example.com/items/7", dayBefore, "some-text-w/o-html", 200)))
  }

  it should "ignore null extracted text" in { f =>
    implicit val actSys = f.actSys
    val daoProbe = TestProbe()
    val feedManager = actSys.actorOf(Props(
      new FeedManager(TestFeedUrl, daoProbe.ref, TestProbe().ref, TestProbe().ref, TestProbe().ref) {
        parsedItemsMap("http://example.com/items/8") = new ParsedFeedItem("http://example.com/items/8", dayBefore)
      }))
    val request = ExtractTextRequest("some-page-html", "http://example.com/items/8", Some(8))
    feedManager ! ExtractTextResponse(null, request)
    daoProbe.expectNoMsg(5 seconds)
  }

  it should "update feed pub date when the last article is persisted" in { f =>
    implicit val actSys = f.actSys
    val daoProbe = TestProbe()
    val feedManager = actSys.actorOf(Props(
      new FeedManager(TestFeedUrl, daoProbe.ref, TestProbe().ref, TestProbe().ref, TestProbe().ref) {
        parsedItemsMap("http://example.com/items/9") = new ParsedFeedItem("http://example.com/items/9", dayBefore)
        feed = new Feed(100, "http://example.com/rss", dayBefore)
        parsedFeed = new ParsedFeed(today, parsedItemsMap("http://example.com/items/9") :: Nil)
      }))
    val article = new Article(9, "http://example.com/items/9", dayBefore, "some-text-w/o-html", 100)
    val feed = new Feed(100, "http://example.com/rss", today)
    feedManager ! PersistArticleResponse(article, PersistArticleRequest(article))
    daoProbe.expectMsg(UpdateFeedRequest(feed))
  }

  it should "wait when persisted article is not the last" in { f =>
    implicit val actSys = f.actSys
    val daoProbe = TestProbe()
    val feedManager = actSys.actorOf(Props(
      new FeedManager(TestFeedUrl, daoProbe.ref, TestProbe().ref, TestProbe().ref, TestProbe().ref) {
        parsedItemsMap("http://example.com/items/9") = new ParsedFeedItem("http://example.com/items/9", dayBefore)
        parsedItemsMap("http://example.com/items/10") = new ParsedFeedItem("http://example.com/items/10", today)
        feed = new Feed(100, "http://example.com/rss", dayBefore)
        parsedFeed = new ParsedFeed(today, Nil)
      }))
    val article = new Article(9, "http://example.com/items/9", dayBefore, "some-text-w/o-html", 100)
    feedManager ! PersistArticleResponse(article, PersistArticleRequest(article))
    daoProbe.expectNoMsg(5 seconds)
  }

  it should "update feed pub date when all articles are processed differently" in { f =>
    implicit val actSys = f.actSys
    val daoProbe = TestProbe()
    val feedManager = actSys.actorOf(Props(
      new FeedManager(TestFeedUrl, daoProbe.ref, TestProbe().ref, TestProbe().ref, TestProbe().ref) {
        // not updated
        parsedItemsMap("http://example.com/items/1") = new ParsedFeedItem("http://example.com/items/1", dayBefore)
        // can't download
        parsedItemsMap("http://example.com/items/2") = new ParsedFeedItem("http://example.com/items/2", null)
        // can't extract text
        parsedItemsMap("http://example.com/items/3") = new ParsedFeedItem("http://example.com/items/3", null)
        // persisted
        parsedItemsMap("http://example.com/items/4") = new ParsedFeedItem("http://example.com/items/4", today)
        feed = new Feed(100, "http://example.com/rss", dayBefore)
        parsedFeed = new ParsedFeed(today, Nil)
      }))
    //
    val oldArticle = new Article(1, "http://example.com/items/1", dayBefore, "some-text-w/o-html", 100)
    feedManager ! ArticleResponse(Some(oldArticle), ArticleRequest("http://example.com/items/1"))
    daoProbe.expectNoMsg(1 seconds)
    //
    feedManager ! ArticlePageResponse(null, ArticlePageRequest("http://example.com/items/2", None))
    daoProbe.expectNoMsg(1 seconds)
    //
    feedManager ! ExtractTextResponse(null, ExtractTextRequest("some-other-html", "http://example.com/items/3", None))
    daoProbe.expectNoMsg(1 seconds)
    //
    val newArticle = new Article(4, "http://example.com/items/4", today, "some-text-w/o-html", 100)
    feedManager ! PersistArticleResponse(newArticle, PersistArticleRequest(newArticle))
    // expect
    val feed = new Feed(100, "http://example.com/rss", today)
    daoProbe.expectMsg(UpdateFeedRequest(feed))
  }

  it should "stop itself if feed is updated" in { f =>
    implicit val actSys = f.actSys
    val observer = TestProbe()
    val feedManager = actSys.actorOf(Props(
      new FeedManager(TestFeedUrl, TestProbe().ref, TestProbe().ref, TestProbe().ref, TestProbe().ref) {
        parsedFeed = new ParsedFeed(today, Nil)
      }))
    observer watch feedManager
    val feed = new Feed(300, "http://example.com/rss", today)
    feedManager ! UpdateFeedResponse(UpdateFeedRequest(feed))
    observer.expectTerminated(feedManager)
  }

  it should "stop itself eventually if it does not receive answer from daoManager" in { f =>
    pending
  }

  it should "stop itself eventually if it does not receive answer from parsingManager" in { f =>
    pending
  }

  it should "stop itself eventually if it does not receive answer from extractionManager" in { f =>
    pending
  }

  protected case class FixtureParam(actSys: ActorSystem)

  override def withFixture(test: OneArgTest) = {
    val actSys = ActorSystem("FeedManagerTest", ConfigFactory.load("test-application"))
    val fp = new FixtureParam(actSys)
    try {
      withFixture(test.toNoArgTest(fp))
    } finally {
      actSys.shutdown()
    }
  }
}

object FeedManagerTest {
  val TestFeedUrl = new URL("http://example.com/rss")

  implicit def string2URL(str: String): URL = new URL(str)

  val today = new Date
  val dayBefore = DateUtils.addDays(today, -1);
}