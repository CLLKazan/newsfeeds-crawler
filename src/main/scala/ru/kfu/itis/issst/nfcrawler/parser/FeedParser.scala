/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.parser
import impl.RomeFeedParser

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
trait FeedParser {
  def parseFeed(content: String): ParsedFeed
}

object FeedParser {
  def getDefault(): FeedParser = new RomeFeedParser
}