/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.dao

import java.net.URL
import java.util.Date

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class Article(val id: Long, val url: URL, val pubDate: Date, val text: String, val feedId: Int) {
  require(id == ID_NOT_PERSISTED || id >= 0, "Illegal article id: %s".format(id))
  require(url != null, "url is null")
  require(text != null, "text is null")
  require(feedId >= 0, "feedID = %s".format(feedId))
}
