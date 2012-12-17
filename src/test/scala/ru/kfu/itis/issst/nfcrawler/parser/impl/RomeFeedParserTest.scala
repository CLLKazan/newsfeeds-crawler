/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.parser.impl
import ru.kfu.itis.issst.nfcrawler.parser
import org.scalatest.FunSuite
import parser.FeedParser
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.time.DateUtils
import java.net.URL
import parser.ParserConfig
import java.util.Calendar

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class RomeFeedParserTest extends FunSuite {

  val feedParser = FeedParser.get(new ParserConfig {})
  assert(feedParser.isInstanceOf[RomeFeedParser])

  test("parse sample feed") {
    val cl = Thread.currentThread.getContextClassLoader()
    val feedContentStream = cl.getResourceAsStream("sample-feed.xml")
    val feedContentString =
      try {
        IOUtils.toString(feedContentStream, "utf-8")
      } finally {
        feedContentStream.close()
      }
    val parsedFeed = feedParser.parseFeed(feedContentString)
    assert(parsedFeed != null)
    assert(parsedFeed.pubDate === null)
    assert(parsedFeed.items.size === 40)
    val parsedItem = parsedFeed.items(1)
    expect(new URL("http://lenta.ru/news/2012/12/12/kandinskyprize/")) {
      parsedItem.url
    }
    assert(parsedItem.pubDate != null)
    /* TODO handle time zone properly to make this test passed on different machines 
    expect(DateUtils.parseDate("2012.12.12 21:31:36 MSK", "yyyy.MM.dd HH:mm:ss z")) {
      parsedItem.pubDate
    }
    */
  }
}