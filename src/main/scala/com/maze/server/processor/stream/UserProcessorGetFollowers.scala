package com.maze.server.processor.stream

import akka.actor.{Actor, ActorRef, Props}
import akka.util.ByteString
import com.maze.server.processor.stream.StreamProcessor.{Command, ProcessGetFollowers}
import com.maze.server.processor.container.FollowersContainer.{FollowersList, GetFollowers}

case object UserProcessorGetFollowers {
  def props(userRef: ActorRef,
            distinationRef: ActorRef,
            keys: List[Int],
            event: ByteString) =
    Props(classOf[UserProcessorGetFollowers], userRef, distinationRef, keys, event)
}
class UserProcessorGetFollowers(userRef: ActorRef,
                                distinationRef: ActorRef,
                                clientIds: List[Int],
                                event: ByteString) extends Actor {

  override def receive: Receive = {

    case ProcessGetFollowers =>
      userRef ! GetFollowers(self)

    case fl: FollowersList =>
      val listCommands = fl.followers.intersect(clientIds).map(id => Command(id, event))
      distinationRef ! listCommands
      context.stop(self)
  }
}
