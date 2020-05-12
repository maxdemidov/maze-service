package com.maze.server.processor.container

import akka.actor.{Actor, ActorRef, Props}
import com.maze.server.processor.container.FollowersContainer._

object FollowersContainer {
  def props(id : Int) = Props(classOf[FollowersContainer], id)

  sealed trait FollowersContainerRequest
  case class Follow(id: Int) extends FollowersContainerRequest
  case class Unfollow(id: Int) extends FollowersContainerRequest
  case class GetFollowers(originalSender: ActorRef) extends FollowersContainerRequest

  sealed trait FollowersContainerResponse
  case class FollowersList(from: Int, followers: Set[Int]) extends FollowersContainerResponse
}
class FollowersContainer(id: Int) extends Actor {

  var followers: Set[Int] = Set()

  override def receive: Receive = {

    case Follow(followerId) =>
      followers = followers + followerId

    case Unfollow(followerId) =>
      followers = followers - followerId

    case GetFollowers(originalSender) =>
      originalSender ! FollowersList(id, followers)
  }
}
