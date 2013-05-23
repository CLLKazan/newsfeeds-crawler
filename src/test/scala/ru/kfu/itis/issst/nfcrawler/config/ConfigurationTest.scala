/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.config

import org.scalatest.FunSuite
import java.net.URL

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
class ConfigurationTest extends FunSuite {

  test("Read configuration from properties file") {
    val config = Configuration.fromPropertiesFile("src/test/resources/config4config-test.properties")
    import config._
    assert(feeds.toSet ===
      Set("http://example.com/rss", "http://example2.com/rss/").map(new URL(_)))
    assert(dbUserName === "foouser")
    assert(dbPassword === "bar")
    assert(dbUrl === "jdbc:someengine:some")
    assert(httpWorkersNumber === 3)
    assert(hostAccessInterval === 1000)
    assert(clientHttpParams === Map(
      "someCoolIntParamName" -> 100,
      "someCoolLongParamName" -> 200l,
      "someCoolBooleanParamName" -> true,
      "someCoolDoubleParamName" -> 2.7d,
      "someCoolStringParamName" -> "someString"))
  }

}