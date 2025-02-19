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
import com.maiden.common.{Geo, Osrm, PubnubHelper, PushNotification}
import com.maiden.common.helpers.Hasher

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
            var geom: String = null,
            var seats: Int = 1,
            var scheduleId: Long = 0,
            var createdAt: Timestamp=new Timestamp(System.currentTimeMillis), 
            var updatedAt: Timestamp=new Timestamp(System.currentTimeMillis)
  ) 
  extends BaseMaidenTableWithTimestamps {

  def getHash() = {
    val h = s"trip-${id}"
    h
    //Hasher.md5(h)
  }

  override def extraMap() = Map(
    "hash" -> getHash(),
    "vehicle" -> {
      Vehicle.get(vehicleId) match {
        case Some(v) => v.asMap
        case _ => Map.empty
      }
    },
    "driver" -> {
      User.get(driverId) match {
        case Some(d) => d.asMap
        case _ => Map.empty
      }
    },
    "pickup" -> {
      Stop.get(pickupStop) match {
        case Some(p) => p.asMap
        case _ => Map.empty
      }
    },
    "dropoff" -> {
      Stop.get(dropoffStop) match {
        case Some(d) => d.asMap
        case _ => Map.empty
      }
    }
  )

  def miniUpdateMap() = Map(
    "id" -> id,
    "rideState" -> rideState,
    "paymentState" -> paymentState,
    "isTransfer" -> isTransfer,
    "pickupStop" -> pickupStop,
    "dropoffStop" -> dropoffStop,
    "isTransfer" -> isTransfer,
    "vehicle" -> {
      Vehicle.get(vehicleId) match {
        case Some(v) => v.asMap
        case _ => Map.empty
      }
    },
    "eta" -> getDriverEta(),
    "hash" -> getHash(),
    "schedule" -> Schedule.get(scheduleId)
  ) 

  def getDriverEta() = {

    if (scheduleId == 0l) {
      //an on-demand trip
      val routeStops = Route.getStops(routeId)
      val vLoc = GpsLocation.getCurrentForUser(driverId).get
      val closest = Stop.getClosestStop(vLoc.latitude, vLoc.longitude)
      val c = Stop.get(closest("id").toString.toLong).get
      val pickup = Stop.get(pickupStop).get 
      val betweenLocs = Trip.getInBetweenStops(routeStops, c, pickup).map(b => {
      val geo = Geo.latLngFromWKB(b.geom)
         (geo("latitude").toFloat, geo("longitude").toFloat)
      }).toList
      val o = Osrm.getRouteAndEta((vLoc.latitude, vLoc.longitude), 
                                    betweenLocs) 

      new DateTime().plusSeconds(o("eta").toString.toInt).toString
    } else {
      //a scheduled trip
      val now = new DateTime()
      val schedule = Schedule.get(scheduleId).get
      val t = schedule.stopTime.split(':')
      if (now.getDayOfWeek == schedule.dayOfWeek) {
        //same day
        now
          .withHourOfDay(t(0).toInt)
          .withMinuteOfHour(t(1).toInt)
          .toString
      } else {
        now
          .plusDays(1)
          .withHourOfDay(t(0).toInt)
          .withMinuteOfHour(t(1).toInt)
          .toString
      }
    }
  }
}

object Trip extends CompanionTable[Trip] {

  implicit def dbl2flt(d: java.lang.Double) = d.toFloat

  val haveRideStates = List(
    RideStateType.VehicleAccepted.id,
    RideStateType.VehicleOnWay.id,
    RideStateType.RideUnderway.id
  )


  def getForDriver(driverId: Long) = fetch {
    from(Trips)(t =>
    where (
      (t.driverId === driverId) and 
      (t.rideState in haveRideStates)
    )
    select(t))
  }
  
  def getPendingForDriver(driverId: Long) = {
    val validStates = List(
      RideStateType.VehicleAccepted.id,
      RideStateType.VehicleOnWay.id
    )

    fetch {
      from(Trips)(t =>
      where (
        (t.driverId === driverId) and 
        (t.rideState in validStates)
      )
      select(t))
    }
  }


  def getExisting(userId: Long) = fetchOne {
    from(Trips)(t => 
    where(
      (t.userId === userId) and 
      (t.rideState in haveRideStates)
    )
    select(t))
  }

  def create(userId: Long, routeId: Long, 
             reservationType: Int, 
             pickupStop: Long, dropoffStop: Long,
             seats: Int = 1, scheduleId: Long) = {

    getExisting(userId) match {
      case Some(t) => throw(new AlreadyHaveTripException()) 
      case _ => {
        val trip = Trip(
          userId = userId,
          routeId = routeId,
          reservationType = reservationType,
          pickupStop = pickupStop,
          dropoffStop = dropoffStop,
          rideState = RideStateType.Initial.id,
          seats = seats,
          scheduleId = scheduleId
        )

        withTransaction {
          Trips.upsert(trip)
          Option(trip)
        }
      }
    }
  }

  def updateState(tripId: Long, state: Int) = {
    //need to validate the transition and do other stuff here
    withTransaction {
      Trip.get(tripId) match {
        case Some(t) => {
          t.rideState = state
          Trips.update(t)
          PubnubHelper.send(t.getHash(), t.asMap)
          t
        }
        case _ => throw(new NoTripException()) 
      }
    }
  }

  def setProcessing(tripId: Long, processing: Boolean) = {
    //need to validate the transition and do other stuff here
    withTransaction {
      update(Trips)(t => 
      where(t.id === tripId)
      set(t.isProcessing := processing))
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
        "dropoff" -> t._8.map(_.asMap),
        "tripGeom" -> {
          if (t._1.geom != null && t._1.geom != "") {
            Geo.latLngListFromWKB(t._1.geom) 
          } else {
            List.empty
          }
        }
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
        (t.reservationTime === reservationTime.?) //BUG
      )
      select(t))
    }
  }

  def getPending() = getByCriteria(
    rideState = Option(RideStateType.Initial.id),
    isProcessing = Option(false)
  )

  def getVehicleOverdue() = {
    val now = new DateTime(System.currentTimeMillis) 
    val tenMinutesAgo = new Timestamp(now.minusMinutes(10).getMillis)
    fetch {
      from(Trips)(t =>  
      where(
        (t.rideState === RideStateType.VehicleAccepted.id) and
        (t.eta lt tenMinutesAgo)
      )
      select(t))
    }
  }

  def getStops(pickupId: Long, dropoffId: Long) = fetchOne {
    from(Stops, Stops)((pickup, dropoff) =>
    where(pickup.id === pickupId and dropoff.id === dropoffId)
    select(pickup, dropoff))
  }

  def getOccupiedTripsByVehicle(vehicleId: Long) = {

    //need to add a time restriction here for reserved rides
    //that are far in the future
    fetch {
      from(Trips)(t =>
      where(
        (t.vehicleId === vehicleId) and
        (t.rideState in haveRideStates)
      )
      select(t))
    }
  }

  def buildOccupancyTable(stops: List[Stop], trips: List[Trip], occupancy: Int, seats: Int) = {
    var table = MMap[Long, Int]()
    stops.foreach(s => table(s.id) = occupancy)
    trips.foreach(t => {
      val pickup = stops.filter(_.id == t.pickupStop).head
      val dropoff = stops.filter(_.id == t.dropoffStop).head
      getInBetweenStops(stops, pickup, dropoff).foreach(s => 
        table(s.id) = table(s.id) - (t.seats + seats) 
      )
    })
    table
  }
  
  def getInBetweenStops(routeStops: List[Stop], pickup: Stop, dropoff: Stop) = {
    if (pickup.stopOrder == dropoff.stopOrder) {
      List.empty
    } else if (pickup.stopOrder < dropoff.stopOrder) {
      routeStops.filter(f => 
          f.stopOrder > pickup.stopOrder &&
          f.stopOrder < dropoff.stopOrder
       )
    } else {
      val after = routeStops.filter(f => f.stopOrder > pickup.stopOrder)
        .sortBy(_.stopOrder)  
      val before = routeStops.filter(f => f.stopOrder < dropoff.stopOrder)
        .sortBy(_.stopOrder)
      after ++ before
    }
  }

  //get the vehicle's occupancy for the given segment 
  def getVehicleAvailability(vehicleId: Long, trip: Trip, 
    pickup: Stop, dropoff: Stop, seats: Int) =  {

    val routeStops = Route.getStops(pickup.routeId)

    val vehicle = Vehicle.get(vehicleId) match {
      case Some(v)  => v
      case _ => throw(new Exception("Invalid Vehicle ID"))
    }

    val trips = getOccupiedTripsByVehicle(vehicle.id)


    //build out the occupancy table
    val occupancy = vehicle.maximumOccupancy
    val occupancyTable = buildOccupancyTable(routeStops, trips, occupancy, seats)
    occupancyTable
  }


  def getAvailableVehicles(trip: Trip, tm: TripMeta, 
                           availabilityTable: Map[Long, MMap[Long, Int]] ) = {

    availabilityTable.filter { 
      case(driver, table) if table.size > 0 => {
        tm.tripStopIds.filter(ts => 
          table(ts) > 0).size ==tm.tripStopIds.size 
      } 
      case _ => false 
    }.map { 
      case(driverId, a)  => 
        tm.vehicles.filter(_.driverId == driverId)
      case _ => List.empty 
    }.toList.flatMap(x=>x) match {
      case x:List[_] if x.size > 0 => Option(x.asInstanceOf[List[Vehicle]])
      case _ => {
        Trip.updateState(trip.id, RideStateType.NoAvailableVehicles.id)
        None
      }
    }   
  }

  def assignVehicle(trip: Trip) = {
    for {
      //trip meta information
      tm <- Option(new TripMeta(trip))

      //the availability of all vehicles
      availabilityTable <- Option(tm.vehicles.map(v => 
            v.driverId -> getVehicleAvailability(v.id, trip, tm.pickup, 
                            tm.dropoff, 
                            trip.seats)).toMap)

      availableVehicles <- getAvailableVehicles(trip, tm, availabilityTable) 
      matchedTrip <- tm.getBestVehicle(availableVehicles)
      bookableTrip <- tm.getInitialTripEtaAndRoute
      bookedTrip <- tm.saveTrip
    } yield(bookedTrip)
  }

  def getTripRoute(trip: Trip) = {
    val pickup = Stop.get(trip.pickupStop).get
    val dropoff = Stop.get(trip.dropoffStop).get
    val tripGeom = Geo.latLngListFromWKB(trip.geom).map(c => 
        List(c("longitude").toString.toFloat, c("latitude").toString.toFloat)
    )
    //val tripGeom = Route.getRouteGeometry(trip.routeId)._1
    Map(
      "geometry" -> tripGeom,
      "stops" -> Map( 
        "pickup" -> pickup.asMap,
        "dropoff" -> dropoff.asMap
      ),
      "vehicle" -> {
        Vehicle.get(trip.vehicleId) match {
          case Some(v) => v.asMap
          case _ => Map.empty
        }
      }
    )
  }

  //a holder for info common amongst all trips
  private class TripMeta(trip: Trip) {
    //all stops on the route
    val routeStops = Route.getStops(trip.routeId)
    //vehicles on the route
    val vehicles = Vehicle.getForRouteRaw(trip.routeId)
    //pickup and dropoff location objects
    val (pickup, dropoff) = 
      getStops(trip.pickupStop, trip.dropoffStop) match {
      case Some((p, d)) => (p,d)
      case _ => throw(new Exception("no stops"))
    }
    val tripStopIds = List(pickup.id) ++ getInBetweenStops(routeStops, pickup, dropoff).map(_.id) ++ List(dropoff.id)


    def getBestVehicle(availableVehicles: List[Vehicle]) = {
      println(availableVehicles)
      availableVehicles.size match {
        case 0 => {
          trip.rideState = RideStateType.NoAvailableVehicles.id 
        }
        case 1 => {
          //assign this vehicle/driver to the trip
          val v = availableVehicles.head
          trip.driverId = v.driverId
          trip.vehicleId = v.id
          trip.rideState = RideStateType.VehicleAccepted.id
        }
        case _ => {
          //TODO: THIS IS INCOMPLETE! need to sort by distance
          val vehicleLocs = availableVehicles.map(v => {
              val loc = GpsLocation.getCurrentForUser(v.driverId) match {
                case Some(gps) => (gps.latitude, gps.longitude)
                case _ => (0f, 0f)
              }
              v.id -> loc 
          }).toMap

          val p = Geo.latLngFromWKB(pickup.geom)
          val stopLoc = (p("latitude").toString.toFloat, p("longitude").toString.toFloat)
        }
      }
      Option(trip)
    }

    def getInitialTripEtaAndRoute() = {
      println("getting ETA")
      //figure out the ETA if successfully booked
      if (trip.rideState == RideStateType.VehicleAccepted.id) {
        val vLoc = GpsLocation.getCurrentForUser(trip.driverId).get
        val closest = Stop.getClosestStop(vLoc.latitude, vLoc.longitude)
        val closestStopLoc = (closest("longitude").toString.toFloat, closest("latitude").toString.toFloat)

        val c = Stop.get(closest("id").toString.toLong).get
        val driverBetweenLocs = (List(c) ++ getInBetweenStops(routeStops, c, pickup) ++ List(pickup)).map(b => {
          val geo = Geo.latLngFromWKB(b.geom)
            (geo("latitude").toFloat, geo("longitude").toFloat)
        }).toList

        //set our ETA on the trip
        trip.eta = new Timestamp(new DateTime(trip.getDriverEta()).getMillis)

        val tripBetweenLocs = (getInBetweenStops(routeStops, pickup, dropoff) ++ List(dropoff)).map(b => {
          val geo = Geo.latLngFromWKB(b.geom)
            (geo("latitude").toFloat, geo("longitude").toFloat)
        }).toList

        val pickupLoc = Geo.latLngFromWKB(pickup.geom) 

        println("trying to get ETA")
        val o2 = Osrm.getRouteAndEta((pickupLoc("latitude").toFloat, pickupLoc("longitude").toFloat), tripBetweenLocs)
        println("here")
        val tripGeom = o2("geometry").asInstanceOf[List[List[Float]]]
        trip.geom = Geo.latLngListToWKB(tripGeom)
        Option(trip)
      } else {
        None
      }
    }

    def saveTrip() = withTransaction {
      trip.isProcessing = false
      Trips.upsert(trip)
      PubnubHelper.send(trip.getHash(), trip.asMap)
      val message = s"${pickup.name} to ${dropoff.name}"
      Notification.send(trip.userId, "Your trip is booked", message )
      Option(trip)
    }
  }
}
