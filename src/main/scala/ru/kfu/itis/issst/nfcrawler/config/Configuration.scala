/**
 *
 */
package ru.kfu.itis.issst.nfcrawler.config
import java.util.Properties
import scala.collection.JavaConversions._
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import ru.kfu.itis.issst.nfcrawler.dao.DaoConfig
import ru.kfu.itis.issst.nfcrawler.http.HttpConfig
import ru.kfu.itis.issst.nfcrawler.parser.ParserConfig
import ru.kfu.itis.issst.nfcrawler.extraction.ExtractionConfig
import java.net.URL

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
trait Configuration extends AnyRef
  with DaoConfig with HttpConfig with ParserConfig with ExtractionConfig {
  val feeds: Set[URL]

  override def toString(): String = ("feeds: %s\n" +
    "hostAccessInterval: %s\n" +
    "httpWorkers: %s\n" +
    "clientHttpParams: %s\n" +
    "dbUrl: %s\n" +
    "dbUsername: %s")
    .format(feeds.mkString("\n\t", "\n\t", ""),
      hostAccessInterval, httpWorkersNumber, clientHttpParams, dbUrl, dbUserName)
}

object Configuration {
  val FeedKeyPrefix = "feed."
  val HostAccessInterval = "http.hostAccessInterval"
  val HttpWorkersNumber = "http.workersNum"
  val HttpClientParamPrefix = "httpClient."
  val DbUrl = "db.url"
  val DbUsername = "db.username"
  val DbPassword = "db.password"

  def fromPropertiesFile(filePath: String): Configuration = {
    val props = new Properties
    val file = new File(filePath)
    if (!file.isFile())
      Predef.error("%s is not a file".format(filePath))
    val fileStream = new FileInputStream(file)
    val fileReader = new InputStreamReader(fileStream, "utf-8")
    try {
      props.load(fileReader)
    } finally {
      fileReader.close()
    }
    // feed urls
    val feedSet = props.stringPropertyNames()
      .filter(_.startsWith(FeedKeyPrefix))
      .map(urlKey => new URL(props.getProperty(urlKey))).toSet
    // http client params
    val httpClientMap = props.stringPropertyNames()
      .filter(_.startsWith(HttpClientParamPrefix))
      .map(hcPropKey =>
        hcPropKey.substring(HttpClientParamPrefix.length) ->
          convertHttpClientPropValue(props.getProperty(hcPropKey)))
      .toMap
    // aux methods
    def getProperty(key: String) = props.getProperty(key)
    def getIntProperty(key: String) = getProperty(key).toInt
    // 
    new Configuration() {
      override val feeds = feedSet
      override val hostAccessInterval = getIntProperty(HostAccessInterval)
      override val httpWorkersNumber = getIntProperty(HttpWorkersNumber)
      override val clientHttpParams = httpClientMap
      override val dbUrl = getProperty(DbUrl)
      override val dbUserName = getProperty(DbUsername)
      override val dbPassword = getProperty(DbPassword)
      override val dbDriverClass = getProperty("com.mysql.jdbc.Driver")
    }
  }

  private val ReIntParam = "(\\d+)i".r
  private val ReLongParam = "(\\d+)l".r
  private val ReDoubleParam = "(\\d+(?:\\.\\d+)?)d".r
  private val ReBooleanParam = "(?i)(true|false)".r
  private def convertHttpClientPropValue(valStr: String): Any = valStr match {
    case ReIntParam(intStr) => intStr.toInt
    case ReLongParam(longStr) => longStr.toLong
    case ReDoubleParam(doubleStr) => doubleStr.toDouble
    case ReBooleanParam(boolStr) => boolStr.toBoolean
    case valStr => valStr
  }
}