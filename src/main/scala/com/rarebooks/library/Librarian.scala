package com.rarebooks.library

import akka.actor.{Actor, ActorLogging, Props, Stash}
import com.rarebooks.library.Librarian.optToEither

import scala.concurrent.duration.FiniteDuration

object Librarian {
  import RareBooksProtocol._

  def props(findBookDuration: FiniteDuration): Props =
    Props(new Librarian(findBookDuration))

  def optToEither[T](v: T, f: T => Option[BookCard]): Either[BookNotFound, BookFound] =
    f(v) match {
      case b: Some[BookCard] =>
        Right(BookFound(List(b.get)))
      case _ =>
        Left(BookNotFound(s"Book(s) not found base on $v"))
    }


}

class Librarian(findBooKDuration: FiniteDuration) extends Actor
  with ActorLogging
  with Stash {

  import RareBooksProtocol._

  override def receive: Receive = {
    // TODO: FindBookByIsbn(isbn, _)
    case f: FindBookByIsbn =>
      /*
        This pattern is common. A requested operation either produces a result or doesn’t,
        and you want to send different messages in response. As Option has subtypes None
        and Some, Either has subtypes Left and Right. The convention is to use Right
        to represent default values and Left to represent failures, so None usually is
        mapped to Left.
       */
      val r = optToEither[String]("", Catalog.books.get)

      // Folding over either is a convenient way to process the alternatives.
      r fold (
        f => sender() ! f, // Book not found
        s => sender() ! s  //  Book found
      )
  }
}