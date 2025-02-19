package com.maiden.data.models

import java.sql.Timestamp
import org.joda.time._
import scala.collection.mutable.ListBuffer
import com.maiden.common.Types._
import com.maiden.common.converters.Convertable
import MaidenSchema._
import com.maiden.common.MaidenCache._
import com.maiden.common.exceptions._
import com.maiden.common.Codes._
import com.maiden.common.Enums.StopType._
import com.maiden.common.{Geo, Osrm}
import com.maiden.common.helpers.Text.camelize


/* this is a quasi model because it uses postgis */
case class Stop(var id: Long = 0,
          var routeId: Long = 0,
          var stopOrder: Int = 0,
          var name: String = "",
          var address: String = "",
          var description: String = "",
          var details: String = "",
          var thumbnail: String = "",
          var geom: String = "",
          var bearing: Float = 0f,
          var bearing_name: String = "",
          var active: Boolean = true,
          var markerType: String = "glass",
          var markerColor: String = "red",
          var showMarker: Boolean = true,
          var closeBy: String = "",
          var createdAt: Timestamp=new Timestamp(System.currentTimeMillis), 
          var updatedAt: Timestamp=new Timestamp(System.currentTimeMillis)) 

  extends BaseMaidenTableWithTimestamps {

  override def extraMap() = {
    val coords = Geo.latLngFromWKB(geom) 
    Map(
      "bearing" -> None,
      "latitude" -> coords("latitude"),
      "longitude" -> coords("longitude"),
      "arringGeofence" -> Geo.generateBoundingBox(
                             coords("latitude").toString.toFloat,
                             coords("longitude").toString.toFloat,
                             300, 300),
       
      "warningGeofence" -> Geo.generateBoundingBox(
                             coords("latitude").toString.toFloat,
                             coords("longitude").toString.toFloat,
                             1000, 1000),
      //these need to be calculated
      "nextArrival" -> new DateTime().plusMinutes(12).toString,
      "arrivals" -> List("11:15", "11:30", "12:00", "12:30"),
      //"schedule" -> Schedule.getForStop(id, routeId),
      "todaysSchedule" -> Schedule.getTodaysSchedule(id)
    )
  }

}

object Stop extends CompanionTable[Stop] {

  private[this] def rawGetStops(routeId: Long) = withTransaction {
    from(Stops)(s =>
    where(s.routeId === routeId)
    select(s)
    orderBy(s.stopOrder))
  }

  def create(routeId: Long, stopOrder: Int = 0, 
             name: String, address: String, details: String,
             description: String, thumbnail: String, geom: List[Float],  
             active: Boolean = true, bearing: Float = 0f,
             markerType: String = "glass", showMarker: Boolean = true,
             closeBy: String = "") = {

    val stops = rawGetStops(routeId) 

    val realStopOrder = if (stopOrder == 0) {
      stops.size + 1
    } else {
      stopOrder
    }

    val geometry = Geo.latLngToWKB(geom)

    val newStop = Stop(routeId = routeId, stopOrder = realStopOrder,
                       name = name, address = address, details = details,
                       description = description, thumbnail = thumbnail,
                       geom = geometry, active = active, bearing = bearing,
                       markerType = markerType, showMarker = showMarker,
                       closeBy = closeBy)

    if (stopOrder == 0) {
      //just a normal insert... eg, push this stop
      withTransaction {
        Stops.upsert(newStop)
      }
    } else {
      //push all the other stops up by 1
      val _stops = stops
                   .filter(s => s.stopOrder >= realStopOrder)
                   .map(s => { s.stopOrder = s.stopOrder + 1; s }) 
                   .foreach(s => withTransaction{ Stops.upsert(s) })

      withTransaction {
        Stops.upsert(newStop)
      }
    }

    //regenerate our bearings
    generateBearings(routeId)

    newStop
  }


  //move a stop to a different stop order 
  def moveStop(routeId: Long, stopId: Long, newStopOrder: Int) = {
    val stops = rawGetStops(routeId)
    val origStop = stops.filter(s => s.id == stopId).head

    if (origStop.stopOrder < newStopOrder) {


    } else {

    }

  }

  def removeStop(routeId: Long, stopId: Long) = {

  }

  def generateBearings(routeId: Long) = {
    val stops = fetch {
      from(Stops)(s => 
      where(s.routeId === routeId)
      select(s)
      orderBy(s.stopOrder))
    }

    val allStops = (stops ++ List(stops(0)))
    allStops.zipWithIndex.foreach{ case (s,i) => { 
      val j = i + 1
      if (j < allStops.size) {
        val stop1 = Geo.latLngFromWKB(s.geom)
        val stop2 = Geo.latLngFromWKB(allStops(j).geom)
        val bearing = Geo.bearing(
                 stop1("latitude"),
                 stop1("longitude"),
                 stop2("latitude"),
                 stop2("longitude"))
        s.bearing = bearing._1.toFloat
        s.bearing_name = bearing._2
        withTransaction {
          Stops.upsert(s)
        }
      }
    }} 
  }

  def getAllStops(routeId: Option[Long] = None, stopId: Option[Long] = None): List[Map[String, Any]] = {
    var sql = """
        select id, route_id, stop_order, name, address, description, 
         details, thumbnail, 
         active, marker_type, marker_color, show_marker,
         close_by, 
         created_at, updated_at,
         ST_Y(geom) as latitude,
         ST_X(geom) as longitude,
         bearing_name
         from stop
    """

    routeId match {
      case Some(id) => sql += s"where route_id = ${id} "
      case _ => ()
    }

    stopId match {
      case Some(id) => sql += s"and id = ${id} "
      case _ => ()
    }
    sql += " order by stop_order "

    var results = ListBuffer[Map[String, Any]]()
    rawQuery(sql, (rs) => {
      var columns = (1 to rs.getMetaData.getColumnCount).map(index =>
          (index -> rs.getMetaData.getColumnName(index))
      ).toMap

      while (rs.next()) {
        results.append(columns.map {
          case(ordinal, name) => (camelize(name) -> rs.getObject(ordinal))
        })
      }
    })
    results.map(r => r ++ Map(
      "arrivalGeofence" -> Geo.generateBoundingBox(
                             r("latitude").toString.toFloat,
                             r("longitude").toString.toFloat,
                             300, 300),
       
      "warningGeofence" -> Geo.generateBoundingBox(
                             r("latitude").toString.toFloat,
                             r("longitude").toString.toFloat,
                             1000, 1000),

      "nextArrival" -> new DateTime().plusMinutes(12).toString,
      "todaysSchedule" -> Schedule.getTodaysSchedule(r("id").toString.toLong)
    )).toList
  }

  def getStop(routeId: Long, stopId: Long):Map[String, Any] = { 
    val stop = getAllStops(Option(routeId), Option(stopId)) match {
      case x: List[_] if x.size == 1 => x(0)
      case _ => throw(new Exception("Stop does not exist")) 
    }

    val stopObj = Stop.get(stop.asInstanceOf[Map[String, Any]]("id").toString.toLong).get

    val dropOffs = Route.stopsBetween(stopObj, stopObj)
    Map(
      "stop" -> stop,
      "dropOffs" -> dropOffs
    )
  }

  def getForRoute(routeId: Long) = getAllStops(Option(routeId)) 

  def getAll() = getAllStops()

  def getClosestStop(lat: Float, lng: Float) = {
    val stops = getAll
    val locs = stops.map(stop =>
      (stop("latitude").toString.toFloat, stop("longitude").toString.toFloat)
    )
    val table = Osrm.getDistanceTable((lat,lng), locs)
    
    val index = table(0).filter(x => x!=0).zipWithIndex.min._2
    stops(index)

  }
}

