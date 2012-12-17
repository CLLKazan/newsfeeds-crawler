/**
 *
 */
package ru.kfu.itis.issst.nfcrawler

import org.scalatest.FunSuite
import http.HttpConfig
import Messages._
import java.net.URL
import scala.actors.Actor
import scala.collection.mutable.LinkedHashSet
import scala.actors.TIMEOUT
import scala.actors.Exit

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class HttpManagerTest extends FunSuite {

  private val man = new HttpManager(new HttpConfig() {
    override val httpWorkersNumber = 3
    override val hostAccessInterval = 500
  })

  test("Concurrent http-client access") {
    man.start()

    val urls = LinkedHashSet(new URL("http://hc.apache.org/httpclient-3.x/threading.html"),
      new URL("http://stackoverflow.com/about"),
      new URL("http://www.scala-lang.org/"))

    import Actor._
    val client = actor {
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

    client !? IsFinished

    man ! Exit(null, Shutdown)

    assert(urls === LinkedHashSet.empty[URL])
  }

  private case object IsFinished
}