/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.http

import ru.kfu.itis.issst.nfcrawler
import impl.DefaultHttpFacade
import java.net.URL
import nfcrawler.util.SimpleFactory

/**
 * @author Rinat Gareev
 *
 */
trait HttpFacade {
  def getContent(url: URL): String
  
  def close()
}

object HttpFacade extends SimpleFactory[HttpConfig, HttpFacade] {
  override protected def defaultBuilder(config: HttpConfig): HttpFacade = new DefaultHttpFacade(config)
}