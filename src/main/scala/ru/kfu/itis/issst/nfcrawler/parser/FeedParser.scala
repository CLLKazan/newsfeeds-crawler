/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.parser
import impl.RomeFeedParser
import ru.kfu.itis.issst.nfcrawler.util.SimpleFactory

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
trait FeedParser {
  def parseFeed(content: String): ParsedFeed
}

object FeedParser extends SimpleFactory[ParserConfig, FeedParser] {
  override protected def defaultBuilder(config: ParserConfig) = new RomeFeedParser
}