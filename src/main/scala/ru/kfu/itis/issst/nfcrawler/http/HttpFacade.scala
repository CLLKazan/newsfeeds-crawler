/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.http

import ru.kfu.itis.issst.nfcrawler.http.impl.DefaultHttpFacade
import java.net.URL

/**
 * @author Rinat Gareev
 *
 */
trait HttpFacade {
  def getContent(url: URL): String
}

object HttpFacade {
  def getDefault(config: HttpConfig): HttpFacade = new DefaultHttpFacade(config)
}