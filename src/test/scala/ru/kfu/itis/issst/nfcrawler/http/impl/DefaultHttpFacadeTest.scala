/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.http.impl

import org.scalatest.FunSuite
import ru.kfu.itis.issst.nfcrawler.http
import http.HttpFacade
import http.HttpConfig
import java.net.URL

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class DefaultHttpFacadeTest extends FunSuite {

  val facade = HttpFacade.get(new HttpConfig() {
    val httpWorkersNumber = 1
    val hostAccessInterval = 1000
  })
  assert(facade.isInstanceOf[DefaultHttpFacade])

  test("get example.com content") {
    val content = facade.getContent(new URL("http://example.com"))
    assert(content.length() > 100)
  }

}