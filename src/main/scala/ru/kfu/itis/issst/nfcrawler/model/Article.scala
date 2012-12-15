/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.model
import java.util.Date
import java.net.URL
import Article._

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class Article(val id: Int, val url: URL, val pubDate: Date, val text: String, val feedId: Int)

object Article {
  val ID_NOT_PERSISTED = -1
}