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
  
  it should "tell HttpManager to download the feed content" in { f =>
    implicit val actSys = f.actSys
    val httpProbe = TestProbe()
    val feedManager = actSys.actorOf(Props(
      new FeedManager(TestFeedUrl, TestProbe().ref, httpProbe.ref, TestProbe().ref, TestProbe().ref)))
    feedManager ! FeedResponse(new Feed(1, TestFeedUrl, null), FeedRequest(TestFeedUrl))
    daoProbe.expectMsg(FeedRequest(TestFeedUrl))
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
}