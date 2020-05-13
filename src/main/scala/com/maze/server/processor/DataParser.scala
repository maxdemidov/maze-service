package com.maze.server.processor

import akka.util.ByteString
import com.maze.server.processor.Model.{Event, EventTypes}

trait DataParser {

  implicit def toInt(str: String): Int = Integer.parseInt(str)
  implicit def toIntOpt(str: String): Option[Int] = Some(Integer.parseInt(str))
  implicit def toByteString(str: String): ByteString = ByteString.apply(str+"\n")

  def parseData(data: ByteString): List[Event] = {
    data.utf8String
      .split("\\n").map(_.trim)
      .map(line => line.split("\\|").toList match {
        case sid :: "F" :: sf :: st :: Nil => Event(line, sid, EventTypes.Follow, sf, st)
        case sid :: "U" :: sf :: st :: Nil => Event(line, sid, EventTypes.Unfollow, sf, st)
        case sid :: "B" :: Nil             => Event(line, sid, EventTypes.Broadcast)
        case sid :: "P" :: sf :: st :: Nil => Event(line, sid, EventTypes.Private, sf, st)
        case sid :: "S" :: sf :: Nil       => Event(line, sid, EventTypes.Status, sf, None)
        case _ =>
          throw new IllegalArgumentException(s"Unsupported data [${data.utf8String}] to convert to event.")
      })
      .sortBy(_.id).toList
  }
}
