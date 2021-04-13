package com.rarebooks.library

import java.lang.System.currentTimeMillis

// Protocol object establishing base messages for all actors
object RareBooksProtocol {
  // Enumerate the topics in the library
  sealed trait Topic
  case object Africa extends Topic
  case object Asia extends Topic
  case object Gilgamesh extends Topic
  case object Greece extends Topic
  case object Persia extends Topic
  case object Philosophy extends Topic
  case object Royalty extends Topic
  case object Tradition extends Topic
  case object Unknown extends Topic

  /**
   * Viable topics for book requests
   */
  val viableTopics: List[Topic] =
    List(Africa, Asia, Gilgamesh, Greece, Persia, Philosophy, Royalty, Tradition)

  /**
   * Book card class.
   *
   * @param isbn the book isbn
   * @param author the book author
   * @param title the book title
   * @param description the book description
   * @param dateOfOrigin the book date of origin
   * @param topic set of associated tags for the book
   * @param publisher the book publisher
   * @param language the language the book is in
   * @param pages the number of pages in the book
   */
  final case class BookCard(isbn: String, author: String, title: String, description: String, dateOfOrigin: String,
                            topic: Set[Topic], publisher: String, language: String, pages: Int)

  // All messages have a time stamp
  trait Msg {
    def dateInMillis: Long
  }

  /**
   * List of book cards found message.
   *
   * @param books list of book cards
   * @param dateInMillis date message was created
   */
  final case class BookFound(books: List[BookCard], dateInMillis: Long = currentTimeMillis) extends Msg {
    require(books.nonEmpty, "Book(s) required.")
  }

  /**
   * Book was not found message.
   *
   * @param reason reason book was not found
   * @param dateInMillis date message was created
   */
  final case class BookNotFound(reason: String, dateInMillis: Long = currentTimeMillis) extends Msg {
    require(reason.nonEmpty, "Reason is required.")
  }

  /**
   * Complain message when book not found.
   *
   * @param dateInMillis date message was created
   */
  final case class Complain(dateInMillis: Long = currentTimeMillis) extends Msg

  /**
   * Find book by isbn message.
   *
   * @param isbn isbn to search for
   * @param dateInMillis date message was created
   */
  case class FindBookByIsbn(isbn: String, dateInMillis: Long = currentTimeMillis) extends Msg {
    require(isbn.nonEmpty, "Isbn required.")
  }

  /**
   * Find book by topic.
   *
   * @param topic set of topics to search for
   * @param dateInMillis date message was created
   */
  final case class FindBookByTopic(topic: Set[Topic], dateInMillis: Long = currentTimeMillis) extends Msg {
    require(topic.nonEmpty, "Topic required.")
  }

  /**
   * Credit message
   *
   * @param dateInMillis date message was created
   */
  final case class Credit(dateInMillis: Long = currentTimeMillis) extends Msg

}
