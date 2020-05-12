package com.maze.server.socket

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}

abstract class SocketServer(socket: InetSocketAddress, processor: ActorRef) extends Actor with ActorLogging {

  import Tcp._
  import context.system

  IO(Tcp) ! Bind(self, socket)

  def getHandlerProps: Props

  override def receive: Receive = {

    case b@Bound(localAddress) =>
      log.info(s"Bound: $b")
      context.parent ! b

    case f@CommandFailed(_: Bind) =>
      log.error(s"Failed: $f")
      context.stop(self)

    case c@Connected(remote, local) =>
      log.info(s"Connected: $c")
      sender() ! Register(context.actorOf(getHandlerProps))
  }
}
