package ru.kfu.itis.issst.nfcrawler

import org.scalatest.fixture.FlatSpec
import akka.actor.Props
import akka.testkit.TestProbe
import akka.actor.PoisonPill

class ProjectManagerTest extends FlatSpec with ActorSystemFixture {

  import ProjectManagerTest._
  import ProjectManager._
  import Messages._

  behavior of "ProjectManager"

  it should "not close project until CloseDoors is received" in { f =>
    implicit val actSys = f.actSys
    val pm = actSys.actorOf(pmProps, "projectManager")
    val testMan = TestProbe()
    val observer = TestProbe()
    observer watch pm
    pm ! RegisterManager(testMan.ref)
    testMan.expectMsg(Initialize)
    testMan.ref ! PoisonPill
    observer.expectNoMsg()
  }

  it should "close project when CloseDoors is received and after that all managers are done" in { f =>
    implicit val actSys = f.actSys
    val pm = actSys.actorOf(pmProps, "projectManager")
    val testMan1 = TestProbe()
    val testMan2 = TestProbe()
    val observer = TestProbe()
    observer watch pm
    pm ! RegisterManager(testMan1.ref)
    pm ! RegisterManager(testMan2.ref)
    pm ! CloseDoors
    testMan1.expectMsg(Initialize)
    testMan2.expectMsg(Initialize)
    testMan1.ref ! PoisonPill
    testMan2.ref ! PoisonPill
    observer.expectTerminated(pm)
  }

  it should "close project when all managers are done and after that CloseDoors is received " in { f =>
    implicit val actSys = f.actSys
    val pm = actSys.actorOf(pmProps, "projectManager")
    val testMan1 = TestProbe()
    val testMan2 = TestProbe()
    val observer = TestProbe()
    observer watch pm
    observer watch testMan1.ref
    observer watch testMan2.ref
    pm ! RegisterManager(testMan1.ref)
    pm ! RegisterManager(testMan2.ref)
    testMan1.expectMsg(Initialize)
    testMan2.expectMsg(Initialize)
    testMan1.ref ! PoisonPill
    testMan2.ref ! PoisonPill
    observer.expectTerminated(testMan1.ref)
    observer.expectTerminated(testMan2.ref)
    //
    pm ! CloseDoors
    //
    observer.expectTerminated(pm)
  }
}

object ProjectManagerTest {
  private val pmProps = Props[ProjectManager]
}