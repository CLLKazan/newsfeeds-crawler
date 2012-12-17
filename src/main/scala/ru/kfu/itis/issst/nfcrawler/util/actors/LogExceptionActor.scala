/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.util.actors

import scala.actors.Actor
import grizzled.slf4j.Logging

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
trait LogExceptionActor extends Actor with Logging {

  override val exceptionHandler: PartialFunction[Exception, Unit] = {
    case ex =>
      error("Exception in actor %s".format(getClass), ex)
      throw ex
  }

}