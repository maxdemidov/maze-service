package com.maze.server.processor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.maze.server.eventsource.EventSourceHandler.{ProcessClean, ProcessData}
import com.maze.server.processor.Model.{Event, EventTypes}
import com.maze.server.processor.UserController.{RegisterHandler, UnRegisterHandler}
import com.maze.server.userclients.UserClientHandler.{RegisterUser, UnRegisterUser}

object EventProcessor {
  def props = Props(classOf[EventProcessor])
}
class EventProcessor extends Actor with DataParser with ActorLogging {

  val empty: Map[Int, ActorRef] = Map[Int, ActorRef]()
  var activeUsers: Map[Int, ActorRef] = empty
  var passiveUsers: Map[Int, ActorRef] = empty

  override def receive: Receive = {

    case ProcessData(data) =>
      parseData(data).foreach {
        case e@Event(_, _, EventTypes.Follow, Some(from), Some(to))   => tellBoth(e, from ,to)
        case e@Event(_, _, EventTypes.Unfollow, Some(from), Some(to)) => tellBoth(e, from ,to)
        case e@Event(_, _, EventTypes.Broadcast, _, _)                => tellEachActive(e)
        case e@Event(_, _, EventTypes.Private, _, Some(to))           => tellActive(e, to)
        case e@Event(_, _, EventTypes.Status, _, _)                   => tellEachActive(e)
        case e =>
          throw new IllegalArgumentException(s"Unsupported event [$e] to resolve destination.")
      }

    case RegisterUser(userId, userRef) =>
      log.debug("Registered user with Id {}", userId)
      passiveUsers.get(userId) match {
        case Some(ref) =>
          passiveUsers = passiveUsers - userId
          activeUsers = activeUsers + (userId -> ref)
        case None =>
          val ref = context.actorOf(UserController.props(userId), "uc-" + userId)
          ref ! RegisterHandler(userRef)
          activeUsers = activeUsers + (userId -> ref)
      }

    case UnRegisterUser(userId) =>
      log.debug("Unregistered user with Id {}", userId)
      activeUsers.get(userId) match {
        case Some(ref) =>
          activeUsers = activeUsers - userId
          passiveUsers = passiveUsers + (userId -> ref)
          ref ! UnRegisterHandler()
        case None =>
      }

    case p@ProcessClean() =>
      if (activeUsers.nonEmpty) self ! p
      else {
        (passiveUsers.values ++ activeUsers.values).foreach(ref => context.stop(ref))
        activeUsers = empty
        passiveUsers = empty
      }
  }

  private def tellBoth(event: Event, from: Int, to: Int): Unit = {
    def sendWithCreate(event: Event, userId: Int): Unit = {
      activeUsers.get(userId).orElse(passiveUsers.get(userId)) match {
        case Some(ref) => ref ! event
        case None =>
          val ref = context.actorOf(UserController.props(userId), "uc-" + userId)
          ref ! event
          passiveUsers = passiveUsers + (userId -> ref)
      }
    }
    sendWithCreate(event, from)
    sendWithCreate(event, to)
  }

  private def tellEachActive(event: Event): Unit = {
    activeUsers.values.foreach(_ ! event)
  }

  private def tellActive(event: Event, to: Int): Unit = {
    activeUsers.get(to).foreach(_ ! event)
  }
}
