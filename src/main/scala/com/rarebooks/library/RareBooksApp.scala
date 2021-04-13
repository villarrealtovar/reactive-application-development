package com.rarebooks.library

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging

import java.io.Console
import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.StdIn

object RareBooksApp extends App {
  val system = ActorSystem("rare-books-system")
  val rareBooksApp: RareBooksApp = new RareBooksApp(system)

}

/**
 * RareBooks bootstrap application.
 *
 * @param system Actor system
 */
class RareBooksApp(system: ActorSystem) extends Console {

  private val log = Logging(system, getClass.getName)
  private val rareBooks = createRareBooks()

  /**
   * Create rarebooks factory method.
   *
   * @return rareBooks ActorRef
   */
  protected def createRareBooks(): ActorRef = {
    system.actorOf(RareBooks.props, "rare-books")
  }

  def run(): Unit = {
    log.warning(f"{} running%nEnter commands [`q` = quit, `2c` = 2 customers, etc.]:", getClass.getSimpleName)
    commandLoop()
    Await.ready(system.whenTerminated, Duration.Inf)
  }

  @tailrec
  private def commandLoop(): Unit = {}
    // TODO: why `Command` throws error
    Command(StdIn.readLine()) match {
      case Command.Customer(count, odds, tolerance) =>
        createCustomer(count, odds, tolerance)
        commandLoop()
      case Command.Quit =>
        system.terminate()
      case Command.Unknown(command) =>
        log.warning(s"Unknown command $command")
        commandLoop()
    }

  /**
   * Create customer factory method.
   *
   * @param count number of customers
   * @param odds chances customer will select a valid topic
   * @param tolerance maximum number of books not found before customer complains
   */
  protected def createCustomer(count: Int, odds: Int, tolerance: Int): Unit =
    for (_ <- 1 to count)
      system.actorOf(Customer.props(rareBooks, odds, tolerance))
}