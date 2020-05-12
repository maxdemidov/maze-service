package com.maze.server.processor.stream

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import com.maze.server.processor.Model.{Event, EventTypes}
import com.maze.server.processor._
import com.maze.server.processor.container.FollowersContainer
import com.maze.server.processor.container.FollowersContainer.{Follow, Unfollow}
import com.maze.server.processor.stream.StreamProcessor.{Command, ProcessGetFollowers, UserEvent}

import scala.concurrent.Future

object CommandResolver {
  def props = Props(classOf[CommandResolver])
}
class CommandResolver extends Actor {

  val log = Logging(context.system, this.getClass)

  val empty: Map[Int, ActorRef] = Map[Int, ActorRef]()
  var followerContainers: Map[Int, ActorRef] = empty

  override def receive: Receive = {

    case ue@UserEvent(_, Event(line, _, EventTypes.Follow, Some(from), Some(to))) =>
      followerContainers.get(to).orElse(instantiation(to)).foreach(_ ! Follow(from))
      sender() ! List(Command(to, line))

    case ue@UserEvent(_, Event(_, _, EventTypes.Unfollow, Some(from), Some(to))) =>
      followerContainers.get(to).orElse(instantiation(to)).foreach(_ ! Unfollow(from))
      sender() ! Nil

    case ue@UserEvent(activeIds, Event(line, _, EventTypes.Broadcast, _, _)) =>
      sender() ! activeIds.toList.map(id => Command(id, line))

    case ue@UserEvent(_, Event(line, _, EventTypes.Private, Some(from), Some(to))) =>
      sender() ! List(Command(to, line))

    case ue@UserEvent(activeIds, Event(line, _, EventTypes.Status, Some(from), _)) =>
      followerContainers.get(from).orElse(instantiation(from)).foreach(ref =>
        context.actorOf(GetFollowersTmp.props(ref, sender(), activeIds, line)) ! ProcessGetFollowers
      )

    case ue@UserEvent(_, event) =>
      Future.failed(throw new IllegalArgumentException(s"Unsupported event [$event] to convert to commands."))

    case UsersProcessClean() =>
      followerContainers.foreach(p => context.stop(p._2))
      followerContainers = empty
  }

  private def instantiation(id: Int) = {
    var ref = context.actorOf(FollowersContainer.props(id), "fc-"+id)
    followerContainers = followerContainers + (id -> ref)
    Some(ref)
  }
}
