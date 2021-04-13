package com.rarebooks.library

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}

import scala.concurrent.duration.{Duration, FiniteDuration, MILLISECONDS => Millis}

object RareBooks {
  case object Close
  case object Open
  case object Report

  def props: Props =
    Props[RareBooks]
}

class RareBooks extends Actor
  with ActorLogging
  with Stash {

  import context.dispatcher
  import RareBooks._
  import RareBooksProtocol._

  // Defines how long various events take in the simulation
  private val openDuration: FiniteDuration =
    Duration(context.system.settings.config.getDuration("rare-books.open-duration", Millis), Millis)

  private val closeDuration: FiniteDuration =
    Duration(context.system.settings.config.getDuration("rare-books.close-duration", Millis), Millis)

  private val findBookDuration: FiniteDuration =
    Duration(context.system.settings.config.getDuration("rare-books.librarian.find-book-duration", Millis), Millis)


  private val librarian = createLibrarian()

  // running totals
  var requestsToday: Int = 0
  var totalRequests: Int = 0

  // Schedules the first close event
  context.system.scheduler.scheduleOnce(openDuration, self, Close)

  /**
   * Set the initial behavior.
   *
   * @return partial function open
   */
  override def receive: Receive = open

  /**
   * Behavior that simulates RareBooks is open.
   *
   * @return partial function for completing the request.
   */
  private def open: Receive = {
    case message: Msg =>
      requestsToday += 1
      // Forwards protocol messages to the Librarian
      librarian forward message
    case Close =>
      context.system.scheduler.scheduleOnce(closeDuration, self, Open)
      context.become(closed)
      self ! Report
  }

  /**
   * Behavior that simulates the RareBooks is closed.
   *
   * @return partial function for completing the request.
   */
  private def closed: Receive = {
    case Report =>
      totalRequests += requestsToday
      log.info(s"$requestsToday requests processed today. Total requests processed = $totalRequests")
      requestsToday = 0
    case Open =>
      context.system.scheduler.scheduleOnce(openDuration, self, Close)
      unstashAll()
      context.become(open)
    case _ =>
      // Stashes other messages that arrive while the shop is closed
      stash()
  }

  /**
   * Create librarian actor.
   *
   * @return librarian ActorRef
   */
  protected def createLibrarian(): ActorRef = {
    context.actorOf(Librarian.props(findBookDuration), "librarian")
  }

}