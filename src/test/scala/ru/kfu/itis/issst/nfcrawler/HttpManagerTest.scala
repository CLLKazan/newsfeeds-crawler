/**
 *
 */
package ru.kfu.itis.issst.nfcrawler

import http.HttpConfig
import Messages._
import java.net.URL
import scala.concurrent.duration._
import scala.concurrent._
import akka.actor.Props
import akka.testkit.TestKit
import akka.actor.ActorSystem
import org.scalatest.FunSuiteLike
import org.scalatest.BeforeAndAfterAll
import scala.collection.mutable.LinkedHashSet
import akka.testkit.ImplicitSender
import com.typesafe.config.ConfigFactory

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class HttpManagerTest(actSys: ActorSystem) extends TestKit(actSys) with ImplicitSender
  with FunSuiteLike with BeforeAndAfterAll {

  def this() {
    this(ActorSystem("HttpManagerTest", ConfigFactory.load("test-application")))
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  private val httpManProps = Props(new HttpManager(new HttpConfig() {
    override val httpWorkersNumber = 3
    override val hostAccessInterval = 500
    override val clientHttpParams = Map(
      "http.connection.timeout" -> 30000)
  }))

  test("Concurrent http-client access") {
    val urls = LinkedHashSet(new URL("http://hc.apache.org/httpclient-3.x/threading.html"),
      new URL("http://stackoverflow.com/about"),
      new URL("http://www.scala-lang.org/"))

    val httpMan = system.actorOf(httpManProps, "httpMan")
    // send
    urls.foreach(httpMan ! new ArticlePageRequest(_, None))
    // expect
    val responses = receiveN(urls.size, 20000 milliseconds) collect {
      case apr: ArticlePageResponse => apr
    }
    assertResult(urls, "Not all article URLs are handled") {
      responses.map(_.request.articleUrl).toSet
    }
    // chech html != null
    for (ArticlePageResponse(html, request) <- responses)
      assert(html != null, s"The response for ${request.articleUrl} contains null html")
  }

  private case object IsFinished
}