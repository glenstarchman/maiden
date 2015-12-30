/*
 * Copyright (c) 2015. Maiden, Inc All Rights Reserved
 */

package com.maiden.api.action

import xitrum.annotation.{GET, POST, First, Swagger}
import xitrum.SkipCsrfCheck
import com.maiden.data.models._
import com.maiden.data.models.MaidenSchema._
import com.maiden.common.converters.ListConverter
import com.maiden.common.MaidenConfigFactory.hostConfig
import com.maiden.common.social.MaidenFacebook
import com.maiden.common.exceptions._
import com.maiden.common.Codes.StatusCode
import com.maiden.common.Enums._

@Swagger(
  Swagger.Tags("Trip", "No Auth"),
  Swagger.Produces("application/json")
)
trait TripApi extends JsonAction 

@Swagger(
  Swagger.Tags("Trip", "Needs Authentication"),
  Swagger.Produces("application/json"),
  Swagger.StringHeader("X-MAIDEN-AT", "Access Token")
)
trait AuthorizedTripApi extends AuthorizedJsonAction

@POST("api/trip/:id")
@GET("api/trip/:id")
@Swagger(
  Swagger.OperationId("get_trip"),
  Swagger.Summary("get a trip's details"),
  Swagger.IntPath("id", "The trip id")
)
class TripInfo extends AuthorizedTripApi {
  def execute() {
    //only the booking user, the assigned driver, or an admin can see
    //for now anyone will do... for testing
    futureExecute(() => { 
      val trip = Trip.getFullDetails(param[Long]("id"))
      val driverId = trip("driverId").toString.toLong
      val userId = trip("userId").toString.toLong
      if (user.get.id == userId || user.get.id == driverId) { 
        (R.OK, trip)
      } else {
        throw(new UnauthorizedException())
      }
    })
  }
}

@POST("api/trip/:id/route")
@GET("api/trip/:id/route")
@Swagger(
  Swagger.OperationId("get_trip_route"),
  Swagger.Summary("get a trip's route"),
  Swagger.IntPath("id", "The trip id")
)
class TripRouteInfo extends AuthorizedTripApi {
  def execute() {
    //only the booking user, the assigned driver, or an admin can see
    //for now anyone will do... for testing
    futureExecute(() => { 
      val trip = Trip.get(param[Long]("id")).get
      val tripGeo = Trip.getTripRoute(trip)
      val driverId = trip.driverId
      val userId = trip.userId
      if (user.get.id == userId || user.get.id == driverId) { 
        (R.OK, tripGeo)
      } else {
        throw(new UnauthorizedException())
      }
    })
  }
}
@First
@POST("api/trip/book")
@GET("api/trip/book")
@Swagger(
  Swagger.OperationId("book_trip"),
  Swagger.Summary("book a trip"),
  Swagger.IntQuery("routeId", "The route id"),
  Swagger.IntQuery("reservationType", "Reservation type"),
  Swagger.IntQuery("pickupStop", "The id of the pickup"),
  Swagger.IntQuery("dropoffStop", "The id of the dropoff"),
  Swagger.OptIntQuery("pickupTime", "Not used yet")
)
class BookTrip extends AuthorizedTripApi {
  def execute() {
    //only the booking user, the assigned driver, or an admin can see
    //for now anyone will do... for testing
    futureExecute(() => { 
      val userId = user.get.id
      Trip.create(userId, 
                  param[Long]("routeId"),
                  param[Int]("reservationType"),
                  param[Long]("pickupStop"),
                  param[Long]("dropoffStop")
       ) match {
         case Some(trip) => (R.OK, trip.asMap)
         case _ => throw(new CreateOrUpdateFailedException())
       }
    })
  }
}

@First
@POST("api/trip/:id/update/state/:state")
@GET("api/trip/:id/update")
@Swagger(
  Swagger.OperationId("update_trip"),
  Swagger.Summary("Update trip data"),
  Swagger.IntPath("id", "The trip id"),
  Swagger.IntPath("state", "The ride state")
)
class UpdateTripState extends AuthorizedTripApi {
  def execute() {
    //only the booking user, the assigned driver, or an admin can see
    //for now anyone will do... for testing
    futureExecute(() => { 
      val userId = user.get.id
      val trip = Trip.get(param[Long]("id")) match {
        case Some(t) => t
        case _ => throw(new NoTripException())
      }
      if (user.get.id == trip.userId || user.get.id == trip.driverId) { 
        //update the state
        val t = Trip.updateState(trip.id, param[Int]("state"))
        (R.OK, t.miniUpdateMap)
      } else {
        throw(new UnauthorizedException())
      }
    })
  }
}



@POST("api/trip/:id/updates")
@GET("api/trip/:id/updates")
@Swagger(
  Swagger.OperationId("trip_updates"),
  Swagger.Summary("get a digest of a trip's updates"),
  Swagger.IntPath("id", "The trip id")
)
class TripUpdates extends AuthorizedTripApi {
  def execute() {
    futureExecute(() => {
      val trip = Trip.get(param[Long]("id")) match {
        case Some(t) => t
        case _ => throw(new NoTripException())
      }
      if (user.get.id == trip.userId || user.get.id == trip.driverId) { 
        (R.OK, trip.miniUpdateMap)
      } else {
        throw(new UnauthorizedException())
      }
    })
  }
}


@POST("api/trip/:id/update/state/:state")
@GET("api/trip/:id/update/state/:state")
@Swagger(
  Swagger.OperationId("update_trip_state"),
  Swagger.Summary("update a trip's state"),
  Swagger.IntPath("id", "The trip id"),
  Swagger.IntPath("state", "The new state")
)
class UpdateTrip extends AuthorizedTripApi {
  def execute() {
    //only the booking user, the assigned driver, or an admin can see
    //for now anyone will do... for testing
    futureExecute(() => { 
      val validStates = RideStateType.values.map(_.id)
      val tripId = param[Long]("id")
      val state = param[Int]("state")

      if (!validStates.contains(state)) {
        throw(new InvalidRideStateException())
      }
      Trip.updateState(tripId, state) match {
        case t: Trip => {
          if (user.get.id == t.userId || user.get.id == t.driverId) { 
            (R.OK, t)
          } else {
            throw(new UnauthorizedException())
          }
        }
        case _ => throw(new NoTripException())
      }
    })
  }
}
