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

    case ue@UserEvent(clientsIds, e) => e match {

      case Event(event, _, EventTypes.Follow, Some(from), Some(to)) =>
        followerContainers.get(to).orElse({
          var ref = context.actorOf(FollowersContainer.props(to), "fc-"+to)
          followerContainers = followerContainers + (to -> ref)
          Some(ref)
        }).foreach(_ ! Follow(from))

        sender() ! List(Command(to, event))

      case Event(event, _, EventTypes.Unfollow, Some(from), Some(to)) =>
        followerContainers.get(to).orElse({
          var ref = context.actorOf(FollowersContainer.props(to), "fc-"+to)
          followerContainers = followerContainers + (to -> ref)
          Some(ref)
        }).foreach(_ ! Unfollow(from))

        sender() ! Nil

      case Event(event, _, EventTypes.Broadcast, _, _) =>

        sender() ! clientsIds.map(id => Command(id, event))

      case Event(event, _, EventTypes.Private, Some(from), Some(to)) =>

        sender() ! List(Command(to, event))

      case Event(event, _, EventTypes.Status, Some(from), _) =>
        followerContainers.get(from).orElse({
          var ref = context.actorOf(FollowersContainer.props(from), "fc-"+from)
          followerContainers = followerContainers + (from -> ref)
          Some(ref)
        }).foreach(ref =>
          context.actorOf(UserProcessorGetFollowers.props(ref, sender(), clientsIds, event)) ! ProcessGetFollowers
        )

      case event =>
        Future.failed(throw new IllegalArgumentException(s"Unsupported event [$event] to convert to commands."))
    }

    case UsersProcessClean() =>
      followerContainers.foreach(p => context.stop(p._2))
      followerContainers = empty
  }
}
