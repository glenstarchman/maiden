
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
import com.maiden.common.{PubnubHelper, Osrm, Geo}

case class GpsLocation(override var id: Long=0, 
                var userId: Long = 0,
                var latitude: Float = 0.0f,
                var longitude: Float = 0.0f,
                var routeId: Long = 0,
                var createdAt: Timestamp=new Timestamp(System.currentTimeMillis), 
                var updatedAt: Timestamp=new Timestamp(System.currentTimeMillis)) 
  extends BaseMaidenTableWithTimestamps {

    override def asMap() = Map(
      "userId" -> userId,
      "routeId" -> routeId,
      "coordinates" -> List(latitude, longitude)
    )
}

object GpsLocation extends CompanionTable[GpsLocation] {

  def getForUser(userId: Long, limit: Int) = fetch {
    from(GpsLocations)(l =>
    where(l.userId === userId)
    select(l)
    orderBy(l.createdAt.desc))
    .page(0,limit)  

  }

  def getCurrentForUser(userId: Long): Option[GpsLocation] = getForUser(userId, 1) match {
    case x: List[_] if x.size > 0 => Option(x(0))
    case _ => None 
  }

  def create(userId: Long, routeId: Long, latitude: Float, longitude: Float) = {
    val gps = GpsLocation(userId = userId, 
                          routeId = routeId,
                          latitude = latitude,
                          longitude = longitude)

    withTransaction {
      GpsLocations.upsert(gps)
    }

   
    Vehicle.getForUser(userId) match {
      case Some(v) => {
        PubnubHelper.send(v.getHash(), gps.asMap)
        PubnubHelper.send("route-" + v.routeId.toString, gps.asMap)
        //for each upcoming trip... update the ETA
        Trip.getPendingForDriver(userId).foreach(t => {
          val routeStops = Route.getStops(t.routeId)
          val closest = Stop.getClosestStop(gps.latitude, gps.longitude)
          val pickup = Stop.get(t.pickupStop).get
          if (closest("id").toString.toLong == pickup.id) {
            val o = Osrm.getRouteAndEta((gps.latitude, gps.longitude),
                                         List((closest("latitude").toString.toFloat, closest("longitude").toString.toFloat)))

            PubnubHelper.send(t.getHash(), Map("eta" -> o("eta").toString.toInt))
          } else {
            val closestStopLoc = (closest("longitude").toString.toFloat, 
                                  closest("latitude").toString.toFloat)
          
            val c = Stop.get(closest("id").toString.toLong).get
            println(c)
            val betweenLocs = Trip.getInBetweenStops(routeStops, c, pickup).map(b => {
              val geo = Geo.latLngFromWKB(b.geom)
              (geo("latitude").toFloat, geo("longitude").toFloat)
            }).toList
            println(betweenLocs)
            val o = Osrm.getRouteAndEta((gps.latitude, gps.longitude),
                                        betweenLocs)
            println("sending ETA for " + t.getHash())
            val eta = o("eta").toString.toInt + (betweenLocs.size * 120)
            PubnubHelper.send(t.getHash(), Map("eta" -> o("eta").toString.toInt))
          }
        })

      }
      case _ => ()
    }


  }

}
