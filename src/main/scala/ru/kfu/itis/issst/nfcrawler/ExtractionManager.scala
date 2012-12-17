/**
 *
 */
package ru.kfu.itis.issst.nfcrawler

import grizzled.slf4j.Logging
import scala.actors.Actor
import extraction.ExtractionConfig
import Messages._
import extraction.TextExtractor
import util.ErrorDumping
import scala.actors.Exit
import ru.kfu.itis.issst.nfcrawler.util.actors.LogExceptionActor

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class ExtractionManager(config: ExtractionConfig) extends LogExceptionActor with Logging with ErrorDumping {

  this.trapExit = true
  override protected val dumpFileNamePattern = "dump-extraction-%s.txt"
  private val textExtractor = TextExtractor.get(config)
  
  override val toString = "ExtractionManager"

  override def act() {
    loop {
      react {
        case msg @ ExtractTextRequest(pageContent, _, _) =>
          debug(msg)
          sender ! ExtractTextResponse(extractText(pageContent), msg)
        case Exit(from, Shutdown) =>
          info("Shutting down...")
          exit(Shutdown)
      }
    }
  }

  private def extractText(htmlSrc: String): String =
    try {
      textExtractor.extractFromHtml(htmlSrc)
    } catch {
      case ex: Exception => {
        val dumpFile = dumpErrorContent(htmlSrc)
        error("Text extraction error. Check dump file %s".format(dumpFile), ex)
        null
      }
    }
}