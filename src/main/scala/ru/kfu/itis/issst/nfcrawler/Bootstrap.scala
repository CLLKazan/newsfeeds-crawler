/**
 *
 */
package ru.kfu.itis.issst.nfcrawler
import ru.kfu.itis.issst.nfcrawler.config.Configuration
import grizzled.slf4j.Logging

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
object Bootstrap extends Logging {

  def main(args: Array[String]) {
    if (args.length != 1)
      Predef.error("Usage: <configFilePath>")
    val config = Configuration.fromPropertiesFile(args(0))
    info("Configuration loaded:\n%s".format(config))
    // create workers
    // -- dao workers
    // -- http workers (fixed pool)
    // -- rss parsers (fixed pool)
    // -- workflow (per feed) managers
    // create managers
    // -- dao layer
    val daoManager = new DaoManager(config)
    // -- http layer
    val httpManager = new HttpManager(config)
    // -- parsing layer
    val parsingManager = new ParsingManager(config)
    // -- feed managers
    val feedManagers = config.feeds.toList.map(
      new FeedManager(_, daoManager, httpManager, parsingManager))

    // start
    daoManager.start()
    parsingManager.start()
    httpManager.start()
    feedManagers.foreach(_.start())
  }

}