
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
import com.maiden.common.PubnubHelper

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
      }
      case _ => ()
    }


  }

}
