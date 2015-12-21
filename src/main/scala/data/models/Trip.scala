/*
 * Copyright (c) 2015. Maiden, Inc All Rights Reserved
*/

package com.maiden.data.models

import java.sql.Timestamp
import scala.collection.mutable.{Map => MMap}
import org.joda.time._
import com.maiden.common.Types._
import MaidenSchema._
import com.maiden.common.MaidenCache._
import com.maiden.common.exceptions._
import com.maiden.common.Codes._
import com.maiden.common.Enums._
import com.maiden.common.{Geo, Osrm}


case class Trip(override var id: Long=0, 
                var userId: Long = 0,
                var driverId: Long = 0,
                var vehicleId: Long = 0,
                var routeId: Long = 0,
                var fareId: Long = 0,
                var reservationType: Int = ReservationType.OnDemand.id,
                var rideState: Int = RideStateType.Initial.id,
                var paymentState: Int = PaymentStateType.Pending.id,
                var discountType: Int = DiscountType.NoDiscount.id,
                var isTransfer: Boolean = false,
                var pickupStop: Long = 0,
                var dropoffStop: Long = 0,
                var reservationTime: Timestamp = new Timestamp(System.currentTimeMillis),
                var eta: Timestamp = null,
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

  def setProcessing(tripId: Long, processing: Boolean) = {
    //need to validate the transition and do other stuff here
    withTransaction {
      Trip.get(tripId) match {
        case Some(t) => {
          t.isProcessing = processing 
          Trips.update(t)
          t
        }
        //don't throw an exception here or the actor will vomit
        case _ => () 
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
                    isProcessing: Option[Boolean] = None,
                    reservationTime: Option[Timestamp] = None) = {

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
        (t.isProcessing === isProcessing.?) and
        (t.reservationTime === reservationTime) //BUG
      )
      select(t))
    }
  }

  def getPending() = getByCriteria(
    rideState = Option( RideStateType.Initial.id),
    isProcessing = Option(false)
  )

  def getStops(pickupId: Long, dropoffId: Long) = fetchOne {
    from(Stops, Stops)((pickup, dropoff) =>
    where(pickup.id === pickupId and dropoff.id === dropoffId)
    select(pickup, dropoff))
  }

  def getOccupiedTripsByVehicle(vehicleId: Long) = {

    val validStates = List(
      RideStateType.VehicleAccepted.id,
      RideStateType.VehicleOnWay.id,
      RideStateType.RideUnderway.id
    )

    //need to add a time restriction here for reserved rides
    //that are far in the future
    fetch {
      from(Trips)(t =>
      where(
        (t.vehicleId === vehicleId) and
        (t.rideState in validStates)
      )
      select(t))
    }
  }


  def buildOccupancyTable(stops: List[Stop], trips: List[Trip], occupancy: Int) = {
    var table = MMap[Long, Int]()
    stops.foreach(s => table(s.id) = occupancy)
    trips.foreach(t => {
      val pickup = stops.filter(_.id == t.pickupStop).head
      val dropoff = stops.filter(_.id == t.dropoffStop).head
      getInBetweenStops(stops, pickup, dropoff).foreach(s => 
        table(s.id) = table(s.id) - 1
      )
    })
    table
  }
  
  def getInBetweenStops(routeStops: List[Stop], pickup: Stop, dropoff: Stop) = {
    if (pickup.stopOrder < dropoff.stopOrder) {
      routeStops.filter(f => 
          f.stopOrder >= pickup.stopOrder &&
          f.stopOrder <= dropoff.stopOrder
       )
    } else {
      routeStops.filter(f => f.stopOrder >= pickup.stopOrder)
        .sortBy(_.stopOrder) ++ 
      routeStops.filter(f => f.stopOrder <= dropoff.stopOrder)
        .sortBy(_.stopOrder)
    }
  }

  //get the vehicle's occupancy for the given segment 
  def getVehicleAvailability(vehicleId: Long, trip: Trip, 
    pickup: Stop, dropoff: Stop) =  {

    val routeStops = Route.getStops(pickup.routeId)

    val betweenStops = getInBetweenStops(routeStops, pickup, dropoff)
    betweenStops.foreach(b => println(b.id, b.name))

    val vehicle = Vehicle.get(vehicleId) match {
      case Some(v) => v
      case _ => throw(new Exception("Invalid Vehicle ID"))
    }

    val trips = getOccupiedTripsByVehicle(vehicle.id)


    //build out the occupancy table
    val occupancy = vehicle.maximumOccupancy
    val occupancyTable = buildOccupancyTable(routeStops, trips, occupancy)
    occupancyTable
  }
                         
  //search for a vehicle that has occupancy for this trip
  //this is only called from DispatchActor
  //and selects the closest driver
  def assignVehicleForOnDemand(trip: Trip) = {

    val routeStops = Route.getStops(trip.routeId)
    if (trip.isProcessing) {
      val vehicles = Vehicle.getForRouteRaw(trip.routeId)
      //need to get vehicles with availability here

      val (pickup, dropoff) = getStops(trip.pickupStop, trip.dropoffStop) match {
        case Some((p, d)) => (p,d)
        case _ => throw(new Exception("no stops"))
      }

      val availabilityTable = vehicles.map(v => 
        v.driverId -> getVehicleAvailability(v.id, trip, pickup, dropoff)
      ).toMap

      val tripStopIds = getInBetweenStops(routeStops, pickup, dropoff).map(_.id)

      val availableVehicles = availabilityTable.filter { case(driver, table) => {
        tripStopIds.filter(ts => table(ts) > 0).size > 0
      }}.map { case(driverId, a)  => vehicles.filter(_.driverId == driverId) }.head 

      //short-circuit if only one vehicle 
      val bookedTrip = availableVehicles.size match {
        case 0 => trip.rideState = RideStateType.NoAvailableVehicles.id 
        case 1 => {
          println("have a single driver")
          //assign this vehicle/driver to the trip
          val v = availableVehicles.head
          trip.driverId = v.driverId
          trip.vehicleId = v.id
          trip.rideState = RideStateType.VehicleAccepted.id
        }
        case _ => {
          //need to sort by distance
          val vehicleLocs = availableVehicles.map(v => {
              val loc = GpsLocation.getCurrentForUser(v.driverId) match {
                case Some(gps) => (gps.longitude, gps.latitude)
                case _ => (0f, 0f)
              }
              v.id -> loc 
          }).toMap

          val p = Geo.latLngFromWKB(pickup.geom)
          val stopLoc = (p("latitude").toString.toFloat, p("longitude").toString.toFloat)
          //get the distance table 
          val distanceTable = Osrm.getDistanceTable(stopLoc, vehicleLocs.values.toList)
          println(distanceTable)
        }
      }

      //figure out the ETA if successfully booked
      if (trip.rideState == RideStateType.VehicleAccepted.id) {
        val vLoc = GpsLocation.getCurrentForUser(trip.driverId).get
        val closest = Stop.getClosestStop(vLoc.latitude, vLoc.longitude)
        val closestStopLoc = (closest("latitude").toString.toFloat, closest("longitude").toString.toFloat)

        val c = Stop.get(closest("id").toString.toLong).get
        val betweenLocs = getInBetweenStops(routeStops, c, pickup).map(b => {
          val geo = Geo.latLngFromWKB(b.geom)
            (geo("longitude").toFloat, geo("latitude").toFloat)
        }).toList

        println(betweenLocs)
                                              
        val o = Osrm.getRouteAndEta((vLoc.longitude, vLoc.latitude), 
                                    betweenLocs) 
        //set our ETA on the trip
        trip.eta = new Timestamp(
          new DateTime().plusSeconds(o("eta").toString.toInt).getMillis
        )
      }

      withTransaction {
        //trip.eta = o("eta").toString.toInt
        Trips.upsert(trip)
      }
    }
  }
  
  def assignVehicleForReservation(trip: Trip) = {

  }


}
