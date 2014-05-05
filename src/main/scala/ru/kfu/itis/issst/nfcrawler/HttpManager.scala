/**
 *
 */
package ru.kfu.itis.issst.nfcrawler
import http.HttpConfig
import http.HttpFacade
import Messages._
import java.net.URL
import scala.collection.{ mutable => muta }
import scala.collection.mutable.DoubleLinkedList
import akka.actor.ActorLogging
import akka.actor.Actor
import akka.actor.ActorRef
import scala.concurrent.duration._
import akka.actor.ReceiveTimeout
import HttpManager._
import akka.actor.Props

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class HttpManager(config: HttpConfig) extends Actor with ActorLogging { manager =>

  private val httpFacade = HttpFacade.get(config)
  private val hostAccessInterval = config.hostAccessInterval

  // keep last access time
  private val hostAccessMap = muta.Map.empty[String, AccessState]

  private val httpWorkersNumber = config.httpWorkersNumber
  require(httpWorkersNumber > 1, "Illegal httpWorkersNumber: %s".format(httpWorkersNumber))
  private val workers =
    for (i <- List.range(1, httpWorkersNumber + 1))
      yield downloader()
  private val freeWorkers = muta.Queue.empty[ActorRef]
  freeWorkers ++= workers

  private var taskList = DoubleLinkedList.empty[DownloadTask]

  override val toString = "HttpManager"

  context.setReceiveTimeout(hostAccessInterval milliseconds)

  override def receive = {
    case msg @ FeedContentRequest(feedUrl) =>
      addTask(feedUrl, sender, new FeedContentResponse(_, msg))
    case msg @ ArticlePageRequest(articleUrl, articleIdOpt) =>
      addTask(articleUrl, sender, new ArticlePageResponse(_, msg))
    case msg @ Downloaded(url, time) =>
      hostAccessMap(url.getHost()) = new AccessPerformed(time)
      handleFreeWorker(taskList, sender)
    case ReceiveTimeout =>
      scanTasks()
  }

  override def postStop() {
    log.info("Shutting down...")
    if (!taskList.isEmpty)
      log.error("Task list is not empty when Stop is got")
    httpFacade.close()
  }

  private def addTask(url: URL, client: ActorRef, replyBuilder: (String) => Any) {
    taskList = taskList append DoubleLinkedList(DownloadTask(url, client, replyBuilder))

    scanTasks()
  }

  private def scanTasks() {
    if (!freeWorkers.isEmpty)
      handleFreeWorker(taskList, freeWorkers.dequeue)
  }

  private def handleFreeWorker(taskList: DoubleLinkedList[DownloadTask], worker: ActorRef) {
    if (taskList.isEmpty) {
      // queue free worker
      freeWorkers += worker
    } else if (isHostAccessAllowed(taskList.elem.url.getHost)) {
      val task = taskList.elem
      // adjust taskList
      taskList.remove()
      if (taskList.prev == null) {
        this.taskList = taskList.next
      }
      // reset host access timer
      hostAccessMap(task.url.getHost) = AccessInProgress
      // send task to worker 
      worker ! task
    } else {
      handleFreeWorker(taskList.next, worker)
    }
  }

  private def isHostAccessAllowed(host: String): Boolean = {
    val hostAccessState = hostAccessMap.get(host) match {
      case Some(as) => as
      case None => AccessNotPerformed
    }
    hostAccessState.timeElapsed >= hostAccessInterval
  }

  private def downloader(): ActorRef = context.actorOf(
    Props(classOf[HttpDownloader], httpFacade))
}

object HttpManager {
  private class HttpDownloader(private val httpFacade: HttpFacade)
    extends Actor with ActorLogging {
    override def receive = {
      case DownloadTask(url, client, replyBuilder) =>
        val content =
          try {
            httpFacade.getContent(url)
          } catch {
            case ex: Exception => {
              log.error(ex, "Error while downloading {}", url)
              null
            }
          }
        val time = System.currentTimeMillis
        client ! replyBuilder(content)
        sender ! Downloaded(url, time)
    }
  }

  private case class Downloaded(url: URL, timestamp: Long)
  private case class DownloadTask(url: URL, client: ActorRef, replyBuilder: (String) => Any)

  private abstract class AccessState {
    def timeElapsed: Long
  }
  private object AccessInProgress extends AccessState {
    override val timeElapsed = 0L
  }
  private class AccessPerformed(from: Long) extends AccessState {
    require(from >= 0)
    override def timeElapsed = System.currentTimeMillis - from
  }
  private object AccessNotPerformed extends AccessState {
    override val timeElapsed = Long.MaxValue
  }
}