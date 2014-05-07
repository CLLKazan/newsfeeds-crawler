/**
 *
 */
package ru.kfu.itis.issst.nfcrawler
import ru.kfu.itis.issst.nfcrawler.config.Configuration
import grizzled.slf4j.Logging
import scala.concurrent.duration._
import scala.concurrent._
import akka.actor.ActorSystem
import akka.actor.Props

/**
 * @author Rinat Gareev (Kazan Federal University)
 *
 */
object Bootstrap extends Logging {

  def main(args: Array[String]) {
    if (args.length != 1)
      Predef.error("Usage: <configFilePath>")
    start(Configuration.fromPropertiesFile(args(0)))
  }

  def start(config: Configuration): ActorSystem = {
    info("Configuration loaded:\n%s".format(config))
    val actSys = ActorSystem("newsfeeds-crawler")
    // create managers
    // -- dao layer
    val daoManager = actSys.actorOf(Props(new DaoManager(config)), "daoManager")
    // -- http layer
    val httpManager = actSys.actorOf(Props(new HttpManager(config)), "httpManager")
    // -- parsing layer
    val parsingManager = actSys.actorOf(Props(new ParsingManager(config)), "parsingManager")
    // -- text extraction layer
    val extractionManager = actSys.actorOf(Props(new ExtractionManager(config)), "extractionManager")
    // create main actor
    val projectManager = actSys.actorOf(Props[ProjectManager])
    // -- feed managers
    import ProjectManager._
    for (url <- config.feeds) {
      val feedManager = actSys.actorOf(Props(
        new FeedManager(feedUrl = url, cfg = config,
          daoManager = daoManager,
          httpManager = httpManager,
          parsingManager = parsingManager,
          extractionManager = extractionManager)), "feedManager:" + toLegalActorName(url.toString))
      projectManager ! RegisterManager(feedManager)
    }
    projectManager ! CloseDoors
    //
    info("Everything is up")
    actSys
  }

  private def toLegalActorName(url: String) = url.replaceAll("[^-\\w:@&=+,.!~*'_;]", "_")
}