
package com.maiden.api.action

import akka.actor.{Actor, ActorRef, Props, Terminated}
import glokka.Registry
import xitrum.{
  Config,
  WebSocketText, WebSocketBinary,
  WebSocketPing, WebSocketPong
}
import xitrum.annotation.WEBSOCKET
import com.maiden.common.Log


object RouteListener {
  val PROXY_NAME = "RouteListener"
  case class ListenRoute(id: Long)
  case class Msg(body: String)
  val registry = Registry.start(Config.actorSystem, PROXY_NAME)
}

class RouteListener extends Actor with Log {
  import RouteListener._

  private var subscribers = Seq.empty[ActorRef]

  def receive = {
    case ListenRoute(id) =>
      val subscriber = sender
      subscribers = subscribers :+ sender
      context.watch(subscriber)
      //msgs foreach (subscriber ! Msg(_))
      println("Joined chat room: " + subscriber)

    case m @ Msg(body) =>
      //msgs = msgs :+ body
      //if (msgs.length > MAX_MSGS) msgs = msgs.drop(1)
      subscribers foreach (_ ! m)

    //case Terminated(subscriber) =>
    //  subscribers = subscribers.filterNot(_ == subscriber)
     // log.debug("Stopped Listening: " + subscriber)
  }
}

@WEBSOCKET("socket/route")
class EchoWebSocketActor extends BaseWebsocketAction {
  def execute() {

    context.become {
      case WebSocketText(text) => {
        respondWebSocketText(text)
      }

      case WebSocketBinary(bytes) =>
        respondWebSocketBinary(bytes)

      case WebSocketPing =>
        // Xitrum automatically sends pong for you,
        // you don't have to send pong yourself

      case WebSocketPong =>
        // Client has received your ping
    }
  }

  override def postStop() {
    log.info("onStop")
    super.postStop()
  }
}
