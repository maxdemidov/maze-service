package com.maze.server.processor

import akka.util.ByteString

object Model {

  object EventTypes extends Enumeration {
    type EventType = Value

    val Follow = Value("F")
    val Unfollow = Value("U")
    val Broadcast = Value("B")
    val Private = Value("P")
    val Status = Value("S")
  }

  case class Event(line: ByteString,
                   id: Int,
                   `type`: EventTypes.EventType,
                   fromUserId: Option[Int] = None,
                   toUserId: Option[Int] = None)
}
