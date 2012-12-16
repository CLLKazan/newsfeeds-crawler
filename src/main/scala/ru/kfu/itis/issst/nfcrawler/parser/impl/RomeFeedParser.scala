/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.parser.impl
import ru.kfu.itis.issst.nfcrawler.parser.{ FeedParser, ParsedFeed, ParsedFeedItem }
import grizzled.slf4j.Logging
import com.sun.syndication.io.SyndFeedInput
import java.io.StringReader
import scala.collection.JavaConversions._
import com.sun.syndication.feed.synd.SyndEntry
import java.net.URL
import java.util.Date

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
private[parser] class RomeFeedParser extends FeedParser with Logging {

  override def parseFeed(contentStr: String): ParsedFeed = {
    val feedInput = new SyndFeedInput
    val feed = feedInput.build(new StringReader(contentStr))
    val parsedFeedItems =
      (for (entryObj <- feed.getEntries(); entry = entryObj.asInstanceOf[SyndEntry])
        yield new ParsedFeedItem(new URL(entry.getLink()), getPubDate(entry)))
        .toList
    new ParsedFeed(feed.getPublishedDate(), parsedFeedItems)
  }

  private def getPubDate(entry: SyndEntry): Date =
    entry.getPublishedDate() match {
      case null => entry.getUpdatedDate()
      case date => date
    }

}