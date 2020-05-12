package com.maze.server.processor.chunk

import akka.actor.{Actor, ActorRef, Props}
import com.maze.server.processor._
import com.maze.server.processor.chunk.UserController.{UserFollow, UserGetFollowers, UserUnfollow}
import com.maze.server.processor.container.FollowersContainer
import com.maze.server.processor.container.FollowersContainer.{Follow, GetFollowers, Unfollow}

object UserController {
  def props = Props(classOf[UserController])

  sealed trait UserControllerCommand

  case class UserFollow(from: Int, to: Int) extends UserControllerCommand
  case class UserUnfollow(from: Int, to: Int) extends UserControllerCommand

  case class UserGetFollowers(from: Int) extends UserControllerCommand
}
class UserController extends Actor {

  val empty: Map[Int, ActorRef] = Map[Int, ActorRef]()
  var followerContainers: Map[Int, ActorRef] = empty

  override def receive: Receive = {

    case UserFollow(from, to) =>
      followerContainers.get(to).orElse(instantiation(to)).foreach(_ ! Follow(from))

    case UserUnfollow(from, to) =>
      followerContainers.get(to).orElse(instantiation(to)).foreach(_ ! Unfollow(from))

    case UserGetFollowers(from) =>
      followerContainers.get(from).orElse(instantiation(from)).foreach(_ ! GetFollowers(sender()))

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
