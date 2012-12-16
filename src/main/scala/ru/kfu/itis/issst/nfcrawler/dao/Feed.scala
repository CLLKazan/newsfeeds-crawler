/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.dao

import java.net.URL
import java.util.Date

/**
 * @author Rinat Gareev
 *
 */
class Feed(val id: Int, val url: URL, val lastPubDate: Date) {
  require(id == ID_NOT_PERSISTED || id >= 0, "Illegal Feed id: %s".format(id))
  require(url != null, "url is null")
}

object Feed {
  def build(url: URL, lastPubDate: Date): Feed = new Feed(ID_NOT_PERSISTED, url, lastPubDate)
}