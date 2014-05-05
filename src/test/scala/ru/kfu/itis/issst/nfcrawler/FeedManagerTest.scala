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