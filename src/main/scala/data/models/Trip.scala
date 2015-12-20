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
                var rideState: Int = RideStateType.FindingDriver.id,
                var paymentState: Int = PaymentStateType.Pending.id,
                var discountType: Int = DiscountType.NoDiscount.id,
                var isTransfer: Boolean = false,
                var pickupStop: Long = 0,
                var dropoffStop: Long = 0,
                var reservationTime: Timestamp = new Timestamp(System.currentTimeMillis),
                var pickupTime: Timestamp = null,
                var dropoffTime: Timestamp = null,
                var cancellationTime: Timestamp = null,
                //var pickupTime: Option[Timestamp] = None,
                //var dropoffTime: Option[Timestamp] = None,
                //var cancellationTime: Option[Timestamp] = None,
                var createdAt: Timestamp=new Timestamp(System.currentTimeMillis), 
                var updatedAt: Timestamp=new Timestamp(System.currentTimeMillis)) 
  extends BaseMaidenTableWithTimestamps {


}

object Trip extends CompanionTable[Trip] {

  def getFullDetails(tripId: Long) = { 
    val trip = fetchOne {
      from(Trips, Users.leftOuter, Users.leftOuter, Vehicles.leftOuter,
        Routes.leftOuter, Fares.leftOuter, Stops.leftOuter, Stops.leftOuter)((trip, user, driver, vehicle, route, fare, pickup, dropoff) =>
      where(
        trip.id === tripId and
        trip.userId === user.map(_.id) and
        trip.driverId === driver.map(_.id) and
        trip.vehicleId === vehicle.map(_.id) and
        trip.routeId === route.map(_.id) and 
        trip.fareId === fare.map(_.id) and
        trip.pickupStop === pickup.map(_.id) and
        trip.dropoffStop === dropoff.map(_.id)
      )
      select(trip, user, driver, vehicle, route, fare, pickup, dropoff))
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

}
