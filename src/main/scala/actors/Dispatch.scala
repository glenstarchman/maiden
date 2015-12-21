package com.maiden.actors

import akka.actor.Actor
import akka.actor.Props
import scala.concurrent.duration._
import scala.collection.mutable.{Map => MMap}
import com.maiden.data.models._
import com.maiden.common.Enums._

case object CheckRideState
case object SendNotifications
case object SendSMS
case object SendEmails
case object HandleBilling

object DispatchActor {

  val system = akka.actor.ActorSystem("system")

  import system.dispatcher

  val dispatchActor = system.actorOf(
    Props(
      new Actor {
        var processingStates = MMap(
          "rideState" -> false,
          "notification" -> false,
          "billing" -> false,
          "sms" -> false,
          "email" -> false
        )

        private def toggleState(key: String) = {
          processingStates(key) = !processingStates(key)
        }

        def receive = {
          case CheckRideState => {
            if (!processingStates("rideState")) {
              toggleState("rideState") 
              Trip.getPending.foreach(trip => {
                //this is where we perform our magic
                Trip.setProcessing(trip.id, true)
                Trip.updateState(trip.id, RideStateType.FindingVehicle.id)
                Trip.assignVehicle(trip)
              })
              toggleState("rideState")
            }
          }
          case SendNotifications => ()
          case HandleBilling => ()
          case SendSMS => ()
          case SendEmails => ()
        }
      }
    )
  )


  def start() = {
    system.scheduler.schedule(0 milliseconds,
                    200 milliseconds,
                    dispatchActor,
                    CheckRideState)

    system.scheduler.schedule(0 milliseconds,
                    300 milliseconds,
                    dispatchActor,
                    SendNotifications)

    system.scheduler.schedule(0 milliseconds,
                    30000 milliseconds,
                    dispatchActor,
                    HandleBilling)
  }
  

}
