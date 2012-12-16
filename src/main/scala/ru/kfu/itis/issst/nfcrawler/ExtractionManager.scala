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

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class ExtractionManager(config: ExtractionConfig) extends Actor with Logging with ErrorDumping {

  override protected val dumpFileNamePattern = "dump-extraction-%s.txt"
  private val textExtractor = TextExtractor.getDefault()

  override def act() {
    loop {
      react {
        case msg @ ExtractTextRequest(pageContent, _, _) =>
          sender ! ExtractTextResponse(extractText(pageContent), msg)
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