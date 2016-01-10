
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
import com.maiden.common.Osrm

@Swagger(
  Swagger.Tags("Route/Stop", "No Auth"),
  Swagger.Produces("application/json")
)
trait RouteApi extends JsonAction

@POST("api/route/:id")
@GET("api/route/:id")
@Swagger(
  Swagger.OperationId("get"),
  Swagger.Summary("retrieves a route by id"),
  Swagger.IntPath("id", "the route id to retrieve")
)
class RouteInfo extends RouteApi {
  def execute() {
    futureExecute(() => {
      val routeId = param[Long]("id")
      //returns Tuple2[geometry, stops]
      val data = Route.getRouteGeometry(routeId)
      val (geometry, stops) = data 
      //val stops = data._2 
      val info = Route.get(routeId).map(_.asMap)
      val vehicles = Vehicle.getForRoute(routeId)
      val ret = Map(
        "info" -> info,
        "stops" -> stops,
        "geometry" -> geometry,
        "vehicles" -> vehicles
      )
      (R.OK, ret) 
    })
  }
}

@POST("api/route/:routeId/stop/:stopId")
@GET("api/route/:routeId/stop/:stopId")
@Swagger(
  Swagger.OperationId("get_route_stop"),
  Swagger.Summary("retrieves a route stop"),
  Swagger.IntPath("routeId", "the route id"),
  Swagger.IntPath("stopId", "the stop id")
)
class RouteStopInfo extends RouteApi {
  def execute() {
    futureExecute(() => {
      val routeId = param[Long]("routeId")
      val stopId = param[Long]("stopId")
      val data = Stop.getStop(routeId, stopId) 
      //returns Tuple2[geometry, stops]
      (R.OK, data) 
    })
  }
}

@First
@POST("api/route/:id/stop/first")
@GET("api/route/:id/stop/first")
@Swagger(
  Swagger.OperationId("get_first_stop"),
  Swagger.Summary("retrieves the first stop on a route")
)
class RouteFirstStop extends RouteApi {
  def execute() {
    futureExecute(() => {
      val stop = Route.getFirstStop(param[Long]("id"))
      stop match {
        case Some(s) => (R.OK, s.asMap)
        case _ => throw(new Exception("no such stop"))
      }
    })
  }
}

@First
@POST("api/route/active")
@GET("api/route/active")
@Swagger(
  Swagger.OperationId("get_active"),
  Swagger.Summary("retrieves all active routes")
)
class AllActiveRoutes extends RouteApi {
  def execute() {
    futureExecute(() => {
      (R.OK, Route.getAllActiveRoutes)
    })
  }
}

@First
@POST("api/route/operating")
@GET("api/route/operating")
@Swagger(
  Swagger.OperationId("get_operating"),
  Swagger.Summary("retrieves all currently operating routes")
)
class AllOperatingRoutes extends RouteApi {
  def execute() {
    futureExecute(() => {
      (R.OK, Route.getCurrentlyActive)
    })
  }
}

@First
@POST("api/route/:id/vehicles")
@GET("api/route/:id/vehicles")
@Swagger(
  Swagger.OperationId("get_vehciles"),
  Swagger.Summary("retrieves all vehicles on a route"),
  Swagger.IntPath("id", "The route id")
)
class RouteVehicles extends RouteApi {
  //this is a polled method so we return only minimal info
  def execute() {
    futureExecute(() => {
      (R.OK, Vehicle.getMinimalForRoute(param[Long]("id")))
    })
  }
}

@First
@POST("api/route/closest/stop")
@GET("api/route/closest/stop")
@Swagger(
  Swagger.OperationId("get_closest_stop"),
  Swagger.Summary("gets the closest stop to a location"),
  Swagger.IntQuery("latitude", "latitude"),
  Swagger.IntQuery("longitude", "longitude")
)
class RouteClosestStop extends RouteApi {
  def execute() {
    futureExecute(() => {
      (R.OK, Stop.getClosestStop(param[Float]("latitude"),
                                 param[Float]("longitude")))
    })
  }
}
