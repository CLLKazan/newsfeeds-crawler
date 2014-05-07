package ru.kfu.itis.issst.nfcrawler.util

import akka.actor.Actor
import akka.event.LogSource
import akka.actor.ActorSystem
import akka.event.DummyClassForStringSources
import akka.event.LoggingAdapter

trait ActorLogging { this: Actor =>

  import ActorLogging._

  private var _log: LoggingAdapter = _

  def log: LoggingAdapter = {
    // only used in Actor, i.e. thread safe
    if (_log eq null)
      _log = akka.event.Logging(context.system, this)
    _log
  }

}

object ActorLogging {
  private implicit val logSource: LogSource[Actor] = new LogSource[Actor] {
    override def genString(a: Actor) = LogSource.fromActor.genString(a)
    override def genString(a: Actor, system: ActorSystem) = LogSource.fromActor.genString(a, system)
    override def getClazz(a: Actor) = classOf[DummyClassForStringSources]
  }
}
