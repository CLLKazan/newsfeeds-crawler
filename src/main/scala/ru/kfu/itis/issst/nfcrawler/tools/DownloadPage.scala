/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.tools
import java.net.URL
import java.io.File
import ru.kfu.itis.issst.nfcrawler
import nfcrawler.http.{ HttpConfig, HttpFacade }
import org.apache.commons.io.FileUtils

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
object DownloadPage {

  def main(args: Array[String]): Unit = {
    if (args.length != 2) error("Usage: <url> <output-file>")
    val url = new URL(args(0))
    val outputFile = new File(args(1))

    val htmlFacade = HttpFacade.get(new HttpConfig() {
      override val httpWorkersNumber = 0
      override val hostAccessInterval = 1000
    })

    val page = htmlFacade.getContent(url)

    FileUtils.writeStringToFile(outputFile, page, "utf-8")
  }

}