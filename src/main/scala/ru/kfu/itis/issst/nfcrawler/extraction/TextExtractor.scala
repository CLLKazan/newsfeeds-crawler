/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.extraction
import ru.kfu.itis.issst.nfcrawler.extraction.impl.BoilerpipeExtractor

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
trait TextExtractor {
  def extractFromHtml(htmlSrc: String): String
}

object TextExtractor {
  def getDefault():TextExtractor = new BoilerpipeExtractor
}