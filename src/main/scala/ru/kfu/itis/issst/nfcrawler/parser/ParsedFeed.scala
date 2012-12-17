/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.parser

import java.net.URL
import java.util.Date
import org.apache.commons.lang3.builder.ToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class ParsedFeed(val pubDate: Date, val items: List[ParsedFeedItem]) {
  override def toString = new ToStringBuilder(ToStringStyle.SHORT_PREFIX_STYLE)
    .append(pubDate).append(items).toString()
}

class ParsedFeedItem(val url: URL, val pubDate: Date) {
  override def toString = new ToStringBuilder(ToStringStyle.SHORT_PREFIX_STYLE)
    .append(url).append(pubDate).toString()
}
