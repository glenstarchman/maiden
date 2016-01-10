
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
import com.maiden.common.{Osrm, Geo}


case class Route(override var id: Long=0, 
                var name: String = "",
                var description: String = "",
                var detail: String = "",
                var hourStart: String = "00:00",
                var hourEnd: String = "00:00",
                var dayStart: Int = 0,
                var dayEnd: Int = 0,
                var active: Boolean = true,
                var thumbnail: String = "",
                var createdAt: Timestamp=new Timestamp(System.currentTimeMillis), 
                var updatedAt: Timestamp=new Timestamp(System.currentTimeMillis)) 
  extends FriendlyIdable with BaseMaidenTableWithTimestamps {


}

object Route extends CompanionTable[Route] {

  def getFirstStop(routeId: Long) = fetchOne {
    from(Stops)(s =>
    where(s.routeId === routeId and s.stopOrder === 1)
    select(s))
  }

  def getStops(routeId: Long) = fetch {
    from(Stops)(s => 
    where(s.routeId === routeId)
    select(s)
    orderBy(s.stopOrder))
  }

  def getStopCount(routeId: Long) = {
    fetchOne {
      from(Stops)(s => 
      where(s.routeId === routeId)
      compute(count(s.routeId)))
    }.head.measures
  }

  //get a list of all stops between start and end
  def stopsBetween(pickup: Stop, dropoff: Stop) = {

    val stopCount = getStopCount(pickup.routeId)

    if (pickup.stopOrder < dropoff.stopOrder) {
      fetch {
        from(Stops)(s => 
        where(
          (s.stopOrder gt pickup.stopOrder) and  
          (s.stopOrder lt dropoff.stopOrder) and
          (s.routeId === pickup.routeId)
        )
        select(s)
        orderBy(s.stopOrder))
      }
    } else {
      //we may be going backward
      fetch {
        from(Stops)(s => 
        where(
          (s.stopOrder gt pickup.stopOrder) and 
          (s.stopOrder lte stopCount) and
          (s.routeId === pickup.routeId)
        )
        select(s)
        orderBy(s.stopOrder))
      } ++ 
      fetch {
        from(Stops)(s => 
        where(
          (s.stopOrder lt dropoff.stopOrder) and
          (s.routeId === pickup.routeId)
        )
        select(s)
        orderBy(s.stopOrder))
      }
    }
  }

  def getRouteGeometry(routeId: Long) = {
    val stops = Stop.getForRoute(routeId)
    val locs = stops.map(stop => (stop("latitude").toString.toFloat, stop("longitude").toString.toFloat))
    val geometry = Osrm.getRoute(locs)
    (geometry, stops)
  }

  def getRouteGeometry(routeId: Long, pickupStop: Stop, dropoffStop: Stop) = {
    val stops = List(pickupStop) ++  stopsBetween(pickupStop, dropoffStop) ++ List(dropoffStop)
    val locs = stops.map(stop => {
      val coords = Geo.latLngFromWKB(stop.geom) 
      (coords("latitude").toString.toFloat, coords("longitude").toString.toFloat)
    })
    val geometry = Osrm.getRouteUnenclosed(locs)
    (geometry, stops)

  }

  def getAllActiveRoutes() = fetch {
    from(Routes)(r => 
    where(r.active === true)
    select(r))
  }

  def getCurrentlyActive() = {
    val now = new DateTime(System.currentTimeMillis);
    val nowDay = now.getDayOfWeek
    val nowHour = now.getHourOfDay
    val nowMinute = now.getMinuteOfHour
    var min = if (nowMinute.toString.length < 2) {
      s"0${nowMinute}"
    } else {
      nowMinute.toString
    }
    var hour = if (nowHour.toString.length < 2) {
      s"0${nowHour}"
    } else {
      nowHour.toString
    }
    val nowTime = s"${hour}:${min}"

    fetch {
      from(Routes)(r => 
      where(
        (nowDay.between(r.dayStart, r.dayEnd)) and
        (nowTime.between(r.hourStart, r.hourEnd)) and
        (r.active === true)
      )
      select(r))
    }
  }
}
