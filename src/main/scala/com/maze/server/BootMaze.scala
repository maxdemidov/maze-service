package com.maze.server

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import java.net.InetSocketAddress

import com.maze.server.eventsource.EventSourceServer
import com.maze.server.processor.chunk.ChunkProcessor
import com.maze.server.processor.stream.StreamProcessor
import com.maze.server.userclients.UserClientsServer

object BootMaze extends App {

  implicit val system = ActorSystem()

  val config: Config = ConfigFactory.load("maze-service.conf")
  lazy val host: String = config.getString("tcp.host")
  lazy val portEventSource: Int = config.getInt("tcp.event-source.port")
  lazy val portUserClients: Int = config.getInt("tcp.user-clients.port")

//  val processorRef = system.actorOf(ChunkProcessor.props)
  val processorRef = system.actorOf(StreamProcessor.props)

  system.actorOf(
    EventSourceServer.props(new InetSocketAddress(host, portEventSource), processorRef)
  )
  system.actorOf(
    UserClientsServer.props(new InetSocketAddress(host, portUserClients), processorRef)
  )
}