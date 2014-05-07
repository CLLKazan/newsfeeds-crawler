package ru.kfu.itis.issst.nfcrawler

import org.scalatest.fixture.Suite
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

trait ActorSystemFixture extends Suite {
  protected case class FixtureParam(actSys: ActorSystem)

  override def withFixture(test: OneArgTest) = {
    val actSys = ActorSystem("FeedManagerTest", ConfigFactory.load("test-application"))
    val fp = new FixtureParam(actSys)
    try {
      withFixture(test.toNoArgTest(fp))
    } finally {
      actSys.shutdown()
    }
  }
}