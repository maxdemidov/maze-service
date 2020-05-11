package com.maze.server.processor

import akka.util.ByteString
import com.maze.server.processor.Model.{Event, EventTypes}

trait DataParser {

  def parseData(data: ByteString): List[Event] = {
    data.utf8String
      .split("\\n").map(_.trim)
      .map(event => event.split("\\|").toList match {

        case sid :: "F" :: sf :: st :: Nil =>
          val id = Integer.parseInt(sid)
          val from = Integer.parseInt(sf)
          val to = Integer.parseInt(st)
          val e = ByteString.apply(event + "\n")
          Event(e, id, EventTypes.Follow, Some(from), Some(to))

        case sid :: "U" :: sf :: st :: Nil =>
          val id = Integer.parseInt(sid)
          val from = Integer.parseInt(sf)
          val to = Integer.parseInt(st)
          val e = ByteString.apply(event + "\n")
          Event(e, id, EventTypes.Unfollow, Some(from), Some(to))

        case sid :: "B" :: Nil =>
          val id = Integer.parseInt(sid)
          val e = ByteString.apply(event + "\n")
          Event(e, id, EventTypes.Broadcast)

        case sid :: "P" :: sf :: st :: Nil =>
          val id = Integer.parseInt(sid)
          val from = Integer.parseInt(sf)
          val to = Integer.parseInt(st)
          val e = ByteString.apply(event + "\n")
          Event(e, id, EventTypes.Private, Some(from), Some(to))

        case sid :: "S" :: sf :: Nil =>
          val id = Integer.parseInt(sid)
          val from = Integer.parseInt(sf)
          val e = ByteString.apply(event + "\n")
          Event(e, id, EventTypes.Status, Some(from), None)

        case _ =>
          throw new IllegalArgumentException(s"Unsupported data [$data] to convert to event.")
      })
      .sortBy(_.id).toList
  }
}
