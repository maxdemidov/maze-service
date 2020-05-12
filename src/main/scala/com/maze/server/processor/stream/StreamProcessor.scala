package com.maze.server.processor.stream

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Source}
import akka.util.{ByteString, Timeout}
import akka.{Done, NotUsed}
import com.maze.server.eventsource.EventSourceHandler.{ProcessClean, ProcessData}
import com.maze.server.processor.Model.Event
import com.maze.server.processor._
import com.maze.server.processor.stream.StreamProcessor.{Command, CommandRef, UserEvent}
import com.maze.server.userclients.UserClientHandler.{RegisterUser, SendToUser, UnRegisterUser}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor, Promise}
import scala.util.{Failure, Success}

object StreamProcessor {
  def props = Props(classOf[StreamProcessor])

  case object ProcessGetFollowers
  case class UserEvent(clientsIds: Set[Int], event: Event)
  case class Command(id: Int, data: ByteString)
  case class CommandRef(ref: ActorRef, data: ByteString)
}
class StreamProcessor extends Actor with ActorLogging with DataParser {

  implicit val timeout: Timeout = Timeout(3 seconds)
  implicit val ec: ExecutionContextExecutor = context.system.dispatcher
  implicit val mat: ActorMaterializer = ActorMaterializer()

  var userClients: Map[Int, ActorRef] = Map[Int, ActorRef]()

  val commandResolver: ActorRef =
    context.actorOf(CommandResolver.props, "command-resolver")

  override def receive: Receive = {

    case ProcessData(data) =>

      val activeIds = userClients.keys.toSet

      val transformToCommands: Flow[ByteString, CommandRef, NotUsed] = Flow[ByteString]
        .map(parseData)
        .flatMapConcat(v => Source(v)
          .map(e => UserEvent(activeIds, e))
          .ask[List[Command]](1)(commandResolver)
        )
        .filter(_.nonEmpty)
        .flatMapConcat(v => Source(toCommandRefs(v)))

      val sendCommands: Flow[CommandRef, Done, NotUsed] = Flow[CommandRef]
        .map(sendCommand)

      val p = Promise[Unit]()
      Source.single(data)
        .via(transformToCommands)
        .via(sendCommands)
        .run()
        .onComplete {
          case Success(_) =>
            p success()
          case Failure(error) =>
            log.error("processing events with error " + error)
            p failure error
        }
      Await.result(p.future, 3 seconds)

    case RegisterUser(userId, userRef) =>
      userClients = userClients + (userId -> userRef)

    case UnRegisterUser(userId) =>
      userClients = userClients - userId

    case ProcessClean() =>
      commandResolver ! UsersProcessClean()
  }

  private def toCommandRefs(commands: List[Command]): List[CommandRef] = {
    commands.map(c => (userClients.get(c.id), c.data)).collect {
      case (Some(ref), d) => CommandRef(ref, d)
    }
  }

  private def sendCommand(commandRefs: CommandRef): Done = {
    commandRefs.ref ! SendToUser(commandRefs.data)
    Done
  }
}
