/**
 *
 */
package ru.kfu.itis.issst.nfcrawler
import ru.kfu.itis.issst.nfcrawler.config.Configuration
import grizzled.slf4j.Logging
import scala.actors.Actor
import scala.actors.Exit
import scala.actors.Exit

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

  def start(config: Configuration) {
    info("Configuration loaded:\n%s".format(config))
    // create managers
    // -- dao layer
    val daoManager = new DaoManager(config)
    // -- http layer
    val httpManager = new HttpManager(config)
    // -- parsing layer
    val parsingManager = new ParsingManager(config)
    // -- text extraction layer
    val extractionManager = new ExtractionManager(config)
    // -- feed managers
    val feedManagers = config.feeds.toList.map(
      new FeedManager(_, daoManager, httpManager, parsingManager, extractionManager))

    // create main actor
    import Actor._
    val mainActor = actor {
      // link all actors
      feedManagers.foreach(link(_))
      List(daoManager, parsingManager, httpManager, extractionManager)
        .foreach(link(_))
      var feedsRemaining = feedManagers.size
      loop {
        react {
          case Exit(from, reason) => {
            val reportFunc = () => "Actor '%s' exited with reason: %s".format(from, reason)
            if ('normal != reason)
              error(reportFunc())
            else info(reportFunc())

            if (from.isInstanceOf[FeedManager])
              feedsRemaining -= 1
            if (feedsRemaining == 0) {
              info("All feeds have been processed. Shutting down managers...")
              exit(Messages.Shutdown)
            }
          }
        }
      }
    }
    mainActor.trapExit = true

    // start
    daoManager.start()
    parsingManager.start()
    httpManager.start()
    extractionManager.start()
    feedManagers.foreach(_.start())

    // block until main actor exit
    val exitActor = actor {
      link(mainActor)
      receive {
        case Exit(mainActor, _) =>
          receive {
            case WaitForFinish => reply(true)
          }
      }
    }
    exitActor.trapExit = true
    exitActor !? WaitForFinish
  }

  private case object WaitForFinish
}