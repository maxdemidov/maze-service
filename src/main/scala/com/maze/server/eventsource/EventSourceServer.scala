package com.maze.server.eventsource

import java.net.InetSocketAddress

import akka.actor.{ActorRef, Props}
import com.maze.server.socket.SocketServer

object EventSourceServer {
  def props(socket: InetSocketAddress, processor: ActorRef) = Props(classOf[EventSourceServer], socket, processor)
}
class EventSourceServer(socket: InetSocketAddress, processor: ActorRef) extends SocketServer(socket, processor) {

  override def getHandlerProps: Props = EventSourceHandler.props(processor)
}
