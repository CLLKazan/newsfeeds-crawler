/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.dao

import java.net.URL
import java.util.Date
import scala.collection.mutable.StringBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

/**
 * @author Rinat Gareev
 *
 */
class Feed(val id: Int, val url: URL, val lastPubDate: Date) {
  require(id == ID_NOT_PERSISTED || id >= 0, "Illegal Feed id: %s".format(id))
  require(url != null, "url is null")

  override def toString = new StringBuilder("Feed[")
    .append(id).append(", ").append(url).append(", ")
    .append(lastPubDate).append("]").toString

  override def equals(obj: Any) = obj match {
    case that: Feed =>
      this.id == that.id &&
        this.url == that.url &&
        this.lastPubDate == that.lastPubDate
    case _ => false
  }

  override def hashCode = new HashCodeBuilder()
    .append(id).append(url).append(lastPubDate)
    .toHashCode
}

object Feed {
  def build(url: URL, lastPubDate: Date): Feed = new Feed(ID_NOT_PERSISTED, url, lastPubDate)
}