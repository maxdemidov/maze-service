package com.maze.server.processor.chunk

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import akka.pattern._
import akka.util.{ByteString, Timeout}
import com.maze.server.eventsource.EventSourceHandler.{ProcessClean, ProcessData}
import com.maze.server.processor.Model.{Event, EventTypes}
import com.maze.server.processor._
import com.maze.server.processor.chunk.UserController.{UserFollow, UserGetFollowers, UserUnfollow}
import com.maze.server.processor.container.FollowersContainer.{FollowersContainerResponse, FollowersList}
import com.maze.server.userclients.UserClientHandler.{RegisterUser, SendToUser, UnRegisterUser}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success}

object ChunkProcessor {
  def props = Props(classOf[ChunkProcessor])
}
class ChunkProcessor extends Actor with DataParser {

  val log = Logging(context.system, this.getClass)

  implicit val timeout: Timeout = Timeout(3 seconds)
  implicit val ec = context.system.dispatcher

  var userClients: Map[Int, ActorRef] = Map[Int, ActorRef]()

  val userController: ActorRef =
    context.actorOf(UserController.props, "user-controller")

  override def receive: Receive = {

    case ProcessData(data) =>

      val promise = Promise[Unit]()

      Future.sequence(parseData(data).map(resolveDestinations)).onComplete({

        case Success(resolved) =>
          processSend(resolved.flatten)
          promise success()

        case Failure(error) =>
          log.error("processing events with error " + error)
          promise failure error
      })

      Await.result(promise.future, 3 seconds)

    case RegisterUser(userId, userRef) =>
      userClients = userClients + (userId -> userRef)

    case UnRegisterUser(userId) =>
      userClients = userClients - userId

    case ProcessClean() =>
      userController ! UsersProcessClean()
  }

  private def resolveDestinations(event: Event): Future[List[(Int, ByteString)]] = event match {

    case Event(bstr, _, EventTypes.Follow, from, to) =>
      userController ! UserFollow(from.get, to.get)
      Future.successful(List((to.get, bstr)))

    case Event(_, _, EventTypes.Unfollow, from, to) =>
      userController ! UserUnfollow(from.get, to.get)
      Future.successful(Nil)

    case Event(bstr, _, EventTypes.Broadcast, _, _) =>
      Future.successful(userClients.keys.toList.map(id => (id, bstr)))

    case Event(bstr, _, EventTypes.Private, from, to) =>
      Future.successful(List((to.get, bstr)))

    case Event(bstr, _, EventTypes.Status, from, _) =>
      (userController ? UserGetFollowers(from.get)).mapTo[FollowersContainerResponse].map({
        case FollowersList(_, followers) =>
          followers.map(id => (id, bstr))
      })

    case e =>
      Future.failed(throw new IllegalArgumentException(s"Unsupported event [$event] to resolve destination."))
  }

  private def processSend(resolved: List[(Int, ByteString)]): Unit = {
    resolved
      .groupBy(_._1)
      .map(kv => kv._1 -> (userClients.get(kv._1), kv._2))
      .values.collect {
        case (Some(ref), bstr) => (ref, bstr)
      }
      .foreach(p =>
        p._2.foreach(v => p._1 ! SendToUser(v._2))
      )
    //          resolved.flatten.map(p => (users.get(p._1), p._2)).collect {
    //            case (Some(actorRef), command) => actorRef ! command
    //          }
  }
}