package com.maze.server.processor.stream

import akka.actor.{Actor, ActorRef, Props}
import akka.util.ByteString
import com.maze.server.processor.stream.StreamProcessor.{Command, ProcessGetFollowers}
import com.maze.server.processor.container.FollowersContainer.{FollowersList, GetFollowers}

case object GetFollowersTmp {
  def props(userRef: ActorRef,
            distRef: ActorRef,
            keys: Set[Int],
            event: ByteString) =
    Props(classOf[GetFollowersTmp], userRef, distRef, keys, event)
}
class GetFollowersTmp(userRef: ActorRef,
                      distRef: ActorRef,
                      activeIds: Set[Int],
                      event: ByteString) extends Actor {

  override def receive: Receive = {

    case ProcessGetFollowers =>
      userRef ! GetFollowers(self)

    case fl: FollowersList =>
      distRef ! activeIds.intersect(fl.followers).toList.map(id => Command(id, event))
      context.stop(self)
  }
}
