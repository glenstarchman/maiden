/*
 * Copyright (c) 2015. Maiden, Inc All Rights Reserved
*/

package com.maiden.data.models

import java.sql.Timestamp
import org.joda.time._
import com.maiden.common.Types._
import MaidenSchema._
import com.maiden.common.MaidenCache._
import com.maiden.common.exceptions._
import com.maiden.common.Codes._
import com.maiden.common.Enums._

case class Trip(override var id: Long=0, 
                var userId: Long = 0,
                var driverId: Long = 0,
                var vehicleId: Long = 0,
                var routeId: Long = 0,
                var fareId: Long = 0,
                var reservationType: Int = ReservationType.Reserved.id,
                var rideState: Int = RideStateType.Initial.id,
                var paymentState: Int = PaymentStateType.Pending.id,
                var discountType: Int = DiscountType.NoDiscount.id,
                var isTransfer: Boolean = false,
                var pickupStop: Long = 0,
                var dropoffStop: Long = 0,
                var reservationTime: Timestamp = new Timestamp(System.currentTimeMillis),
                var pickupTime: Timestamp = null,
                var dropoffTime: Timestamp = null,
                var cancellationTime: Timestamp = null,
                var isProcessing: Boolean = false,
                var createdAt: Timestamp=new Timestamp(System.currentTimeMillis), 
                var updatedAt: Timestamp=new Timestamp(System.currentTimeMillis)) 
  extends BaseMaidenTableWithTimestamps {


}

object Trip extends CompanionTable[Trip] {


  def create(userId: Long, reservationType: Int, 
             pickupStop: Long, dropoffStop: Long) = {
    val trip = Trip(
      userId = userId,
      reservationType = reservationType,
      pickupStop = pickupStop,
      dropoffStop = dropoffStop,
      rideState = RideStateType.Initial.id
    )

    withTransaction {
      Trips.upsert(trip)
    }
  }

  def updateState(tripId: Long, state: Int) = {
    //need to validate the transition and do other stuff here
    withTransaction {
      Trip.get(tripId) match {
        case Some(t) => {
          t.rideState = state
          Trips.update(t)
          t
        }
        case _ => throw(new NoTripException()) 
      }
    }
  }

  def getFullDetails(tripId: Long) = { 
    val trip = fetchOne {
      join(Trips, Users.leftOuter, Users.leftOuter, Vehicles.leftOuter,
          Routes.leftOuter, Fares.leftOuter, 
          Stops.leftOuter, Stops.leftOuter)(
            (trip, user, driver, vehicle, route, fare, pickup, dropoff) =>
      where(trip.id === tripId)
      select(trip, user, driver, vehicle, route, fare, pickup, dropoff)
      on(
        trip.userId === user.map(_.id),
        trip.driverId === driver.map(_.id),
        trip.vehicleId === vehicle.map(_.id),
        trip.routeId === route.map(_.id),
        trip.fareId === fare.map(_.id),
        trip.pickupStop === pickup.map(_.id),
        trip.dropoffStop === dropoff.map(_.id)
      ))
    }

    trip match {
      case Some(t) => t._1.asMap ++ Map(
        "user" -> t._2.map(_.asMap),
        "driver" -> t._3.map(_.asMap),
        "vehicle" -> t._4.map(_.asMap),
        "route" -> t._5.map(_.asMap),
        "fare" -> t._6.map(_.asMap),
        "pickup" -> t._7.map(_.asMap),
        "dropoff" -> t._8.map(_.asMap)
      )
      case _ => Map[String, Any]()
    }
  }

  def getByCriteria(userId: Option[Long] = None,
                    driverId: Option[Long] = None,
                    vehicleId: Option[Long] = None,
                    pickupStop: Option[Long] = None,
                    dropoffStop: Option[Long] = None,
                    rideState: Option[Int] = None,
                    paymentState: Option[Int] = None,
                    isProcessing: Option[Boolean] = None) = {

    fetch {
      from(Trips)(t => 
      where(
        (t.userId === userId.?) and
        (t.driverId === driverId.?) and
        (t.vehicleId === vehicleId.?) and
        (t.pickupStop === pickupStop.?) and
        (t.dropoffStop === dropoffStop.?) and
        (t.rideState === rideState.?) and
        (t.paymentState === paymentState.?) and
        (t.isProcessing === isProcessing.?)
      )
      select(t))
    }
  }

  def getPending() = getByCriteria(rideState = Option( RideStateType.Initial.id))
                         

  //grab all trips that need to be assigned
  def assignVehicle() = {
    val trips = getByCriteria(
      rideState = Option(RideStateType.Initial.id)
    ) 

  }

}
