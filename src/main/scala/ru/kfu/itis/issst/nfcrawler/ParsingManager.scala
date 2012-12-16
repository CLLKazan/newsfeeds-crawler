/**
 *
 */
package ru.kfu.itis.issst.nfcrawler
import scala.actors.Actor
import grizzled.slf4j.Logging
import ru.kfu.itis.issst.nfcrawler.parser.ParserConfig
import Messages._
import parser.ParsedFeed
import org.apache.commons.io.FileUtils
import parser.FeedParser
import java.io.File
import ParsingManager._
import ru.kfu.itis.issst.nfcrawler.util.ErrorDumping

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class ParsingManager(config: ParserConfig) extends Actor with Logging with ErrorDumping {

  override protected val dumpFileNamePattern = DumpFilePattern
  val feedParser = FeedParser.getDefault()

  override def act() {
    loop {
      react {
        case msg @ FeedParsingRequest(feedContent) =>
          sender ! FeedParsingResponse(parseFeed(feedContent), msg)
        case Stop => exit()
      }
    }
  }

  private def parseFeed(feedContent: String): ParsedFeed =
    try {
      feedParser.parseFeed(feedContent)
    } catch {
      case ex: Exception => {
        val dumpFile = dumpErrorContent(feedContent)
        error("Feed parsing error. Check content dump in file %s".format(dumpFile), ex)
        null
      }
    }

}

object ParsingManager {
  private val DumpFilePattern = "parsing-error-dump-%s.txt"
}