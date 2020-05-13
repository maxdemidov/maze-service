package com.maze.server.processor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.maze.server.processor.Model.{Event, EventTypes}
import com.maze.server.processor.UserController.{RegisterHandler, UnRegisterHandler}
import com.maze.server.userclients.UserClientHandler.SendToUser

object UserController {
  def props(userId: Int) = Props(classOf[UserController], userId)

  sealed trait UserControllerCommand
  case class RegisterHandler(handlerRef: ActorRef) extends UserControllerCommand
  case class UnRegisterHandler() extends UserControllerCommand
}
class UserController(userId: Int) extends Actor {

  val empty: Set[Int] = Set[Int]()
  var followedOn: Set[Int] = empty
  var clientRef: Option[ActorRef] = None

  override def receive: Receive = {

    case Event(line, _, EventTypes.Follow, Some(from), Some(to)) =>
      if (userId == from)
        followedOn = followedOn + to
      else
        clientRef.foreach(_ ! SendToUser(line))

    case Event(_, _, EventTypes.Unfollow, Some(from), Some(to)) =>
      if (userId == from)
        followedOn = followedOn - to

    case Event(line, _, EventTypes.Broadcast, _, _) =>
      clientRef.foreach(_ ! SendToUser(line))

    case Event(line, _, EventTypes.Private, _, _) =>
      clientRef.foreach(_ ! SendToUser(line))

    case Event(line, _, EventTypes.Status, Some(from), _) =>
      if (followedOn(from))
        clientRef.foreach(_ ! SendToUser(line))

    case RegisterHandler(handlerRef) =>
      clientRef = Some(handlerRef)

    case UnRegisterHandler() =>
      clientRef = None
  }
}
