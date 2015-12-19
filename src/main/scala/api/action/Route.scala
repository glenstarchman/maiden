
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
      val stops = Stop.getForRoute(routeId)
      val locs = stops.map(stop => (stop("latitude").toString.toFloat, stop("longitude").toString.toFloat))
      val geometry = Osrm.getRoute(locs)
      val info = Route.get(routeId)
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
class AllRouteVehicles extends RouteApi {
  def execute() {
    futureExecute(() => {
      (R.OK, Vehicle.getForRoute(param[Long]("id")))
    })
  }
}
