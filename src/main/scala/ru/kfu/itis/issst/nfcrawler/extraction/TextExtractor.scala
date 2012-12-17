/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.extraction
import ru.kfu.itis.issst.nfcrawler.extraction.impl.BoilerpipeExtractor
import ru.kfu.itis.issst.nfcrawler.util.SimpleFactory

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
trait TextExtractor {
  def extractFromHtml(htmlSrc: String): String
}

object TextExtractor extends SimpleFactory[ExtractionConfig, TextExtractor] {
  override protected def defaultBuilder(config: ExtractionConfig) = new BoilerpipeExtractor
}