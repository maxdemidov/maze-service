package com.maze.server.userclients

import java.net.InetSocketAddress

import akka.actor.{ActorRef, Props}
import com.maze.server.socket.SocketServer

object UserClientsServer {
  def props(socket: InetSocketAddress, processor: ActorRef) = Props(classOf[UserClientsServer], socket, processor)
}
class UserClientsServer(socket: InetSocketAddress, processor: ActorRef) extends SocketServer(socket, processor) {

  override def getHandlerProps: Props = UserClientHandler.props(processor)
}
