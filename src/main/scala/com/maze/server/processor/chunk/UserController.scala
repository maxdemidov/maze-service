package com.maze.server.processor.chunk

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
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

  val log = Logging(context.system, this.getClass)

  val empty: Map[Int, ActorRef] = Map[Int, ActorRef]()
  var followerContainers: Map[Int, ActorRef] = empty

  override def receive: Receive = {

    case UserFollow(from, to) =>
      followerContainers.get(to).orElse({
        var ref = context.actorOf(FollowersContainer.props(to), "fc-"+to)
        followerContainers = followerContainers + (to -> ref)
        Some(ref)
      }).foreach(_ ! Follow(from))

    case UserUnfollow(from, to) =>
      followerContainers.get(to).orElse({
        var ref = context.actorOf(FollowersContainer.props(to), "fc-"+to)
        followerContainers = followerContainers + (to -> ref)
        Some(ref)
      }).foreach(_ ! Unfollow(from))

    case UserGetFollowers(from) =>
      followerContainers.get(from).orElse({
        var ref = context.actorOf(FollowersContainer.props(from), "fc-"+from)
        followerContainers = followerContainers + (from -> ref)
        Some(ref)
      }).foreach(_ ! GetFollowers(sender()))

    case UsersProcessClean() =>
      followerContainers.foreach(p => context.stop(p._2))
      followerContainers = empty
  }
}
