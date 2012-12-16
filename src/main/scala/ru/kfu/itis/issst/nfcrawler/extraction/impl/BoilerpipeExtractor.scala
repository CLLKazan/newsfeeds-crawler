/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.extraction.impl
import ru.kfu.itis.issst.nfcrawler.extraction.TextExtractor
import grizzled.slf4j.Logging
import de.l3s.boilerpipe.extractors.ArticleExtractor

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
private[extraction] class BoilerpipeExtractor extends TextExtractor with Logging {

  override def extractFromHtml(htmlSrc: String): String = {
    ArticleExtractor.INSTANCE.getText(htmlSrc)
  }

}