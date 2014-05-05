/**
 *
 */
package ru.kfu.itis.issst.nfcrawler

import akka.actor.Actor
import akka.actor.ActorLogging
import scala.collection.mutable.ListBuffer
import akka.actor.ActorRef
import akka.actor.Terminated
import scala.collection.{ mutable => mu }

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class ProjectManager extends Actor with ActorLogging {
  import ProjectManager._

  private val managers = mu.Set.empty[ActorRef]
  // count manager for logging
  private var managerCounter = 0
  private var doorClosed = false

  override def receive = {
    case RegisterManager(manRef) =>
      managers += manRef
      managerCounter += 1
      context watch manRef
      log.info("Director {} has arrived")
      manRef ! Messages.Initialize
    case CloseDoors =>
      doorClosed = true
      log.info("Watching over {} managers", managerCounter)
    case Terminated(manRef) =>
      if (!(managers remove manRef)) {
        log.warning("Unknown manager reference: {}", manRef)
      }
      if (managers.isEmpty && doorClosed) closeProject()
  }

  private def closeProject() {
    log.info("Closing.")
    context.system.shutdown()
  }

}

object ProjectManager {
  case class RegisterManager(ref: ActorRef)
  case object CloseDoors
}