package com.maze.server.userclients

import akka.actor.{Actor, ActorRef, Props}
import akka.io.Tcp
import akka.util.ByteString
import com.maze.server.userclients.UserClientHandler.{RegisterUser, SendToUser, UnRegisterUser}

object UserClientHandler {
  def props(processor: ActorRef) = Props(classOf[UserClientHandler], processor)

  sealed trait UserClientHandlerCommand
  case class RegisterUser(userId: Int, userRef: ActorRef) extends UserClientHandlerCommand
  case class UnRegisterUser(userId: Int) extends UserClientHandlerCommand
  case class SendToUser(data: ByteString) extends UserClientHandlerCommand
}
class UserClientHandler(processor: ActorRef) extends Actor {

  var userId: Option[Integer] = None
  var origin: ActorRef = _

  import Tcp._

  def init: Receive = {

    case Received(data) =>
      userId = Some(Integer.parseInt(data.utf8String.trim))
      userId.foreach(id =>
        processor ! RegisterUser(id, self)
      )
      origin = sender()
      context.become(process orElse init)

    case PeerClosed =>
      userId.foreach(id =>
        processor ! UnRegisterUser(id)
      )
      context.stop(self)
  }

  def process: Receive = {

    case SendToUser(data) =>
      origin ! Write(data)
  }

  override def receive: Receive = init
}
