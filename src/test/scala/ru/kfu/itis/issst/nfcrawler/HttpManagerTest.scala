/**
 *
 */
package ru.kfu.itis.issst.nfcrawler

import org.scalatest.FunSuite
import http.HttpConfig
import Messages._
import java.net.URL
import scala.collection.mutable.LinkedHashSet
import scala.concurrent.duration._
import scala.concurrent._
import akka.actor.Props

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class HttpManagerTest extends FunSuite {

  private val manProps = Props(new HttpManager(new HttpConfig() {
    override val httpWorkersNumber = 3
    override val hostAccessInterval = 500
    override val clientHttpParams = Map(
      "http.connection.timeout" -> 30000)
  }))

  test("Concurrent http-client access") {
    val urls = LinkedHashSet(new URL("http://hc.apache.org/httpclient-3.x/threading.html"),
      new URL("http://stackoverflow.com/about"),
      new URL("http://www.scala-lang.org/"))

    val client = ActorDSL.actor(new Actor {
      def act() {
        urls.foreach(man ! new ArticlePageRequest(_, None))

        loop {
          reactWithin(20000) {
            case ArticlePageResponse(html, request) =>
              if (html != null) urls -= request.articleUrl
              if (urls.isEmpty || html == null)
                receive {
                  case IsFinished =>
                    reply(true)
                    exit
                }
            case TIMEOUT => receive {
              case IsFinished =>
                reply(true)
                exit
            }
          }
        }
      }
    })

    implicit val timeout = Timeout(36500 days)
    Await.result(client ? IsFinished, Duration.Inf)

    man ! Exit(null, Shutdown)

    assert(urls === LinkedHashSet.empty[URL])
  }

  private case object IsFinished
}