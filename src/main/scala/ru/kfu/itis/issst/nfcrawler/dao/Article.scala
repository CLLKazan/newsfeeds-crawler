/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.dao

import java.net.URL
import java.util.Date
import scala.collection.mutable.StringBuilder

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class Article(val id: Long, val url: URL, val pubDate: Date, val text: String, val feedId: Int) {
  require(id == ID_NOT_PERSISTED || id >= 0, "Illegal article id: %s".format(id))
  require(url != null, "url is null")
  require(text != null, "text is null")
  require(feedId >= 0, "feedID = %s".format(feedId))

  override def toString = new StringBuilder("Article[")
    .append(id).append(", ")
    .append(url).append(", ")
    .append(pubDate).append(", ")
    .append(feedId).append(", ")
    .append(if (text == null) text else text.substring(0, Math.min(200, text.length())))
    .append("]").toString()
}
