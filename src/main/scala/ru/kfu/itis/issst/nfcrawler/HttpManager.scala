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
import scala.actors.OutputChannel
import scala.collection.mutable.DoubleLinkedList
import scala.actors.TIMEOUT
import scala.actors.Exit
import util.actors.LogExceptionActor

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class HttpManager(config: HttpConfig) extends LogExceptionActor with Logging { manager =>

  this.trapExit = true
  private val httpFacade = HttpFacade.get(config)
  private val hostAccessInterval = config.hostAccessInterval

  // keep last access time
  private val hostAccessMap = muta.Map.empty[String, AccessState]

  private val httpWorkersNumber = config.httpWorkersNumber
  require(httpWorkersNumber > 1, "Illegal httpWorkersNumber: %s".format(httpWorkersNumber))
  private val workers =
    for (val i <- List.range(1, httpWorkersNumber + 1))
      yield downloader()
  private val freeWorkers = muta.Queue.empty[OutputChannel[Any]]
  freeWorkers ++= workers

  private var taskList = DoubleLinkedList.empty[DownloadTask]
  
  override val toString = "HttpManager"

  override def act() {
    loop {
      reactWithin(hostAccessInterval) {
        case msg @ FeedContentRequest(feedUrl) =>
          debug(msg)
          addTask(feedUrl, sender, new FeedContentResponse(_, msg))
        case msg @ ArticlePageRequest(articleUrl, articleIdOpt) =>
          debug(msg)
          addTask(articleUrl, sender, new ArticlePageResponse(_, msg))
        case msg @ Downloaded(url, time) =>
          debug(msg)
          hostAccessMap(url.getHost()) = new AccessPerformed(time)
          handleFreeWorker(taskList, sender)
        case msg @ TIMEOUT =>
          debug(msg)
          scanTasks()
        case Exit(from, Shutdown) =>
          info("Shutting down...")
          if (!taskList.isEmpty) error("Task list is not empty when Stop is got")
          httpFacade.close()
          exit(Shutdown)
      }
    }
  }

  private def addTask(url: URL, client: OutputChannel[Any], replyBuilder: (String) => Any) {
    taskList = taskList append DoubleLinkedList(DownloadTask(url, client, replyBuilder))

    scanTasks()
  }

  private def scanTasks() {
    if (!freeWorkers.isEmpty)
      handleFreeWorker(taskList, freeWorkers.dequeue)
  }

  private def handleFreeWorker(taskList: DoubleLinkedList[DownloadTask], worker: OutputChannel[Any]) {
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

  private def downloader(): Actor = Actor.actor {
    Actor.link(manager)
    Actor.loop {
      Actor.react {
        case msg @ DownloadTask(url, client, replyBuilder) =>
          debug(msg)
          val content =
            try {
              httpFacade.getContent(url)
            } catch {
              case ex: Exception => {
                error("Error while downloading %s".format(url), ex)
                null
              }
            }
          val time = System.currentTimeMillis
          client ! replyBuilder(content)
          manager ! Downloaded(url, time)
      }
    }
  }
}

private case class Downloaded(url: URL, timestamp: Long)
private case class DownloadTask(url: URL, client: OutputChannel[Any], replyBuilder: (String) => Any)

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