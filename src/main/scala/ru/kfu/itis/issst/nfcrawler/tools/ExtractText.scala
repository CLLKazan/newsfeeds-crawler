/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.tools
import java.io.File
import org.apache.commons.io.FileUtils
import ru.kfu.itis.issst.nfcrawler
import nfcrawler.extraction.{ TextExtractor, ExtractionConfig }

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
object ExtractText {

  def main(args: Array[String]): Unit = {
    if (args.length != 1) {
      error("Usage: <input-file>")
    }
    val inputFile = new File(args(0))

    val extractor = TextExtractor.get(new ExtractionConfig() {})

    val inputContent = FileUtils.readFileToString(inputFile, "utf-8")

    val result = extractor.extractFromHtml(inputContent)

    println(result)
  }

}