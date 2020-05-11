package com.maze.server.processor.stream

import akka.actor.{Actor, ActorRef, Props}
import akka.util.ByteString
import com.maze.server.processor.stream.StreamProcessor.{Command, ProcessGetFollowers}
import com.maze.server.processor.container.FollowersContainer.{FollowersList, GetFollowers}

case object UserProcessorGetFollowers {
  def props(userRef: ActorRef,
            distRef: ActorRef,
            keys: Set[Int],
            event: ByteString) =
    Props(classOf[UserProcessorGetFollowers], userRef, distRef, keys, event)
}
class UserProcessorGetFollowers(userRef: ActorRef,
                                distRef: ActorRef,
                                activeIds: Set[Int],
                                event: ByteString) extends Actor {

  override def receive: Receive = {

    case ProcessGetFollowers =>
      userRef ! GetFollowers(self)

    case fl: FollowersList =>
      val listCommands = fl.followers.intersect(activeIds).map(id => Command(id, event))
      distRef ! listCommands
      context.stop(self)
  }
}
