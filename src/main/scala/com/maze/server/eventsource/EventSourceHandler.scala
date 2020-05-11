package com.maze.server.eventsource

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import akka.io.Tcp
import akka.util.ByteString
import com.maze.server.eventsource.EventSourceHandler.{ProcessClean, ProcessData}

object EventSourceHandler {
  def props(processor: ActorRef) = Props(classOf[EventSourceHandler], processor)

  sealed trait EventSourceHandlerCommand
  case class ProcessData(data: ByteString) extends EventSourceHandlerCommand
  case class ProcessClean() extends EventSourceHandlerCommand
}
class EventSourceHandler(processor: ActorRef) extends Actor {

  val log = Logging(context.system, this.getClass)

  import Tcp._

  override def receive: Receive = {

    case Received(data) =>
      processor ! ProcessData(data)

    case PeerClosed     =>
      processor ! ProcessClean()
      context.stop(self)
  }
}