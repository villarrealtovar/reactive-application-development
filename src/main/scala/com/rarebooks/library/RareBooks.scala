package com.rarebooks.library

import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props, Stash, SupervisorStrategy}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}

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

  // Overrides the default supervision strategy
  override val supervisorStrategy: SupervisorStrategy = {

    // Creates a Decider for the strategy
    val decider: SupervisorStrategy.Decider = {
      case Librarian.ComplainException(complain, customer) => // Decides what to do with a ComplainException
        customer ! Credit()
        SupervisorStrategy.Restart // Invokes the Restart directive
    }

    OneForOneStrategy() (decider orElse super.supervisorStrategy.decider ) // Returns the OneForOneStrategy with the decider or applies the default strategy

  }

  // Defines how long various events take in the simulation
  private val openDuration: FiniteDuration =
    Duration(context.system.settings.config.getDuration("rare-books.open-duration", Millis), Millis)

  private val closeDuration: FiniteDuration =
    Duration(context.system.settings.config.getDuration("rare-books.close-duration", Millis), Millis)

  private val findBookDuration: FiniteDuration =
    Duration(context.system.settings.config.getDuration("rare-books.librarian.find-book-duration", Millis), Millis)

  private val maxComplainCount: Int = context.system.settings.config getInt "rare-books.librarian.max-complain-count"

  // Gets the number of Librarian routees to create from the configuration
  private val nbrOfLibrarians: Int = context.system.settings.config getInt "rare-books.nbr-of-librarians"

  // Local mutable reference is a Router instead of an ActorRef
  private var router: Router = createLibrarian()

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
      router.route(message, sender()) //Routes the message instead of forwarding
      requestsToday += 1
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
   * Create librarian as router.
   *
   * @return librarian router reference
   */
  protected def createLibrarian(): Router = {
    var cnt: Int = 0
    val routees: Vector[ActorRefRoutee] = Vector.fill(nbrOfLibrarians) {
      val r = context.actorOf(Librarian.props(findBookDuration, maxComplainCount), s"librarian-$cnt")
      cnt += 1
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

}
