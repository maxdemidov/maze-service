package com.maze.server.processor.stream

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}
import akka.util.{ByteString, Timeout}
import akka.{Done, NotUsed}
import com.maze.server.eventsource.EventSourceHandler.{ProcessClean, ProcessData}
import com.maze.server.processor.Model.{Event, EventTypes}
import com.maze.server.processor._
import com.maze.server.processor.stream.StreamProcessor.{Command, CommandRef, UserEvent}
import com.maze.server.userclients.UserClientHandler.{RegisterUser, SendToUser, UnRegisterUser}

import scala.concurrent.duration._
import scala.concurrent.{Await, Promise}
import scala.util.{Failure, Success}

object StreamProcessor {
  def props = Props(classOf[StreamProcessor])

  case object ProcessGetFollowers
  case class UserEvent(clientsIds: List[Int], event: Event)
  case class Command(id: Int, data: ByteString)
  case class CommandRef(ref: ActorRef, data: ByteString)
}
class StreamProcessor extends Actor with DataParser {

  val log = Logging(context.system, this.getClass)

  implicit val timeout: Timeout = Timeout(2 seconds)
  implicit val ec = context.system.dispatcher

  implicit val mat = ActorMaterializer()

  var userClients: Map[Int, ActorRef] = Map[Int, ActorRef]()

  val commandResolver: ActorRef =
    context.actorOf(CommandResolver.props, "command-resolver")

  override def receive: Receive = {

    case ProcessData(data) =>

      val clientsIds = userClients.keys.toList

      val transformToCommands: Flow[ByteString, CommandRef, NotUsed] =
        Flow[ByteString]
          .map(parseData)
          .flatMapConcat(v => Source(v)
            .map(e => UserEvent(clientsIds, e))
            .ask[List[Command]](1)(commandResolver)
          )
          .filter(_.nonEmpty)
          .flatMapConcat(v => Source(commandsToCommandRefs(v)))

      val sendCommands: Flow[CommandRef, Done, NotUsed] =
        Flow[CommandRef]
          .map(sendCommand)

      //      val sink: Sink[List[CommandRef], Future[Done]] = Sink.   runWith(Sink.ignore)

      val p = Promise[Unit]()
      Source.single(data).via(transformToCommands).via(sendCommands).run().onComplete {
        case Success(_) => p success()
        case Failure(error) => p failure error
      }
      Await.result(p.future, 2 seconds)

    case RegisterUser(userId, userRef) =>
      userClients = userClients + (userId -> userRef)

    case UnRegisterUser(userId) =>
      userClients = userClients - userId

    case ProcessClean() =>
      commandResolver ! UsersProcessClean()
  }

  private def commandsToCommandRefs(commands: List[Command]): List[CommandRef] = {
    commands.map(c => (userClients.get(c.id), c.data)).collect {
      case (Some(ref), d) => CommandRef(ref, d)
    }
  }

  private def sendCommand(commandRefs: CommandRef): Done = {
    commandRefs.ref ! SendToUser(commandRefs.data)
    Done
  }
}
