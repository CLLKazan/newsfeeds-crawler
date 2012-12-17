/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.extraction.impl

import org.scalatest.FunSuite
import ru.kfu.itis.issst.nfcrawler.extraction
import extraction._
import org.apache.commons.io.IOUtils

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class BoilerpipeTextExtractorTest extends FunSuite {

  val extractor = TextExtractor.get(new ExtractionConfig {})
  assert(extractor.isInstanceOf[BoilerpipeExtractor])

  test("extract news message") {
    val cl = Thread.currentThread.getContextClassLoader()
    val htmlStream = cl.getResourceAsStream("extractor-test.html")
    val htmlString =
      try {
        IOUtils.toString(htmlStream, "utf-8")
      } finally {
        htmlStream.close()
      }
    val result = extractor.extractFromHtml(htmlString)
    assert(result.contains("Д'Орсонья"))
  }
}