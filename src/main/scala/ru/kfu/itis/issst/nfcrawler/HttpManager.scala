/**
 *
 */
package ru.kfu.itis.issst.nfcrawler
import scala.actors.Actor
import grizzled.slf4j.Logging
import http.HttpConfig
import http.HttpFacade
import Messages._
import java.net.URL
import scala.collection.{ mutable => muta }

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class HttpManager(config: HttpConfig) extends Actor with Logging {
  private val httpFacade = HttpFacade.getDefault(config)
  private val hostAccessInterval = config.hostAccessInterval
  private val hostAccessMap = muta.Map.empty[String, Long]

  override def act() {
    loop {
      react {
        case msg @ FeedContentRequest(feedUrl) => download(feedUrl) match {
          case Downloaded(content) => sender ! FeedContentResponse(content, msg)
          case DownloadError => sender ! FeedContentResponse(null, msg)
          case Postponed => // TODO XXX
        }
      }
    }
  }

  private def download(url: URL): DownloadResult = {
    val host = url.getHost
    val hostLastAccess = hostAccessMap.get(host) match {
      case Some(ts) => ts
      case None => Long.MinValue
    }
    if (System.currentTimeMillis - hostLastAccess >= hostAccessInterval) {
      try {
        val content = httpFacade.getContent(url)
        if (content == null)
          DownloadError
        else Downloaded(content)
      } catch {
        case ex: Exception => {
          error("Error while downloading %s".format(url), ex)
          DownloadError
        }
      }
    } else {
      Postponed
    }
  }
}

private sealed trait DownloadResult
private case class Downloaded(content: String) extends DownloadResult
private case object Postponed extends DownloadResult
private case object DownloadError extends DownloadResult