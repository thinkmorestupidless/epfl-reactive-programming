/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package actorbintree

import akka.actor._
import scala.collection.immutable.Queue

object BinaryTreeSet {

  trait Operation {
    def requester: ActorRef
    def id: Int
    def elem: Int
  }

  trait OperationReply {
    def id: Int
  }

  /** Request with identifier `id` to insert an element `elem` into the tree.
    * The actor at reference `requester` should be notified when this operation
    * is completed.
    */
  case class Insert(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request with identifier `id` to check whether an element `elem` is present
    * in the tree. The actor at reference `requester` should be notified when
    * this operation is completed.
    */
  case class Contains(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request with identifier `id` to remove the element `elem` from the tree.
    * The actor at reference `requester` should be notified when this operation
    * is completed.
    */
  case class Remove(requester: ActorRef, id: Int, elem: Int) extends Operation

  /** Request to perform garbage collection*/
  case object GC

  /** Holds the answer to the Contains request with identifier `id`.
    * `result` is true if and only if the element is present in the tree.
    */
  case class ContainsResult(id: Int, result: Boolean) extends OperationReply
  
  /** Message to signal successful completion of an insert or remove operation. */
  case class OperationFinished(id: Int) extends OperationReply

}


class BinaryTreeSet extends Actor {
  import BinaryTreeSet._
  import BinaryTreeNode._

  def createRoot: ActorRef = context.actorOf(BinaryTreeNode.props(0, initiallyRemoved = false))

  var root = createRoot

  // optional
  var pendingQueue = Queue.empty[Operation]

  // optional
  def receive = normal

  // optional
  /** Accepts `Operation` and `GC` messages. */
  val normal: Receive = {
    case m: Contains => root ! m
    case m: Insert => root ! m
    case m: Remove => root ! m
  }

  // optional
  /** Handles messages while garbage collection is performed.
    * `newRoot` is the root of the new binary tree where we want to copy
    * all non-removed elements into.
    */
  def garbageCollecting(newRoot: ActorRef): Receive = ???

}

object BinaryTreeNode {
  trait Position

  case object Left extends Position
  case object Right extends Position

  case class CopyTo(treeNode: ActorRef)
  case object CopyFinished

  def props(elem: Int, initiallyRemoved: Boolean) = Props(classOf[BinaryTreeNode],  elem, initiallyRemoved)
}

class BinaryTreeNode(val elem: Int, initiallyRemoved: Boolean) extends Actor with ActorLogging {
  import BinaryTreeNode._
  import BinaryTreeSet._

  var subtrees = Map[Position, ActorRef]()
  var removed = initiallyRemoved

  // optional
  def receive = normal

  def insert(position: Position, elemToInsert: Int): Unit = {

  }

  // optional
  /** Handles `Operation` messages and `CopyTo` requests. */
  val normal: Receive = {
    case Insert(requester, id, elemToInsert) =>
      log.info("INSERT {}, {}, {}", id, elem, elemToInsert)
      if (elemToInsert == elem) {
        requester ! OperationFinished(id)
      } else {
        val position: Position = if (elemToInsert < elem) Left else Right
        if (subtrees.contains(position)) {
          subtrees(position) ! Insert(requester, id, elemToInsert)
        } else {
          subtrees += position -> context.actorOf(props(elemToInsert, false))
          requester ! OperationFinished(id)
        }
      }

    case Contains(requester, id, elemToSearch) =>
      log.info("CONTAINS => {}, {}, {}", id, elem, elemToSearch)
      if (elemToSearch == elem) {
        log.info("contains {} => {}", elemToSearch, removed)
        requester ! ContainsResult(id, !removed)
      } else {
        val position: Position = if (elemToSearch < elem) Left else Right
        if (subtrees.contains(position)) {
          subtrees(position) ! Contains(requester, id, elemToSearch)
        } else {
          requester ! ContainsResult(id, false)
        }
      }

    case Remove(requester, id, elemToRemove) =>
      log.info("REMOVE {}, {}, {}", id, elem, elemToRemove)
      if (elemToRemove == elem) {
        removed = true
        requester ! OperationFinished(id)
      } else {
        val position: Position = if (elemToRemove == elem) Left else Right
        if (subtrees.contains(position)) {
          subtrees(position) ! Remove(requester, id, elemToRemove)
        } else {
          requester ! OperationFinished(id)
        }
      }
  }

  // optional
  /** `expected` is the set of ActorRefs whose replies we are waiting for,
    * `insertConfirmed` tracks whether the copy of this node to the new tree has been confirmed.
    */
  def copying(expected: Set[ActorRef], insertConfirmed: Boolean): Receive = ???


}
