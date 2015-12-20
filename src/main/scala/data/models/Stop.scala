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
import com.maiden.common.Osrm


/* this is a quasi model because it uses postgis */
case class Stop(var id: Long = 0,
          var routeId: Long = 0,
          var stopOrder: Int = 0,
          var name: String = "",
          var address: String = "",
          var description: String = "",
          var details: String = "",
          var thumbnail: String = "",
          var latitude: Float = 0.00f,
          var longitude: Float = 0.00f
) extends Convertable {
  override def extraMap() = Map(
    "nextArrival" -> new DateTime().plusMinutes(12).toString,
    "arrivals" -> List("11:15", "11:30", "12:00", "12:30")
   )

}

object Stop {

  def getAllStops(routeId: Option[Long]): List[Map[String, Any]] = {
    var sql = routeId match {
      case Some(id) => s"""
        select id, route_id, stop_order, name, address, description, 
         details, thumbnail,
         ST_X(geom) as latitude,
         ST_Y(geom) as longitude
         from stop
         where route_id = ${id}
         order by stop_order asc
         """
      case _ => s"""
         select id, route_id, stop_order, name, address, description, 
         details, thumbnail,
         ST_X(geom) as latitude,
         ST_Y(geom) as longitude
         from stop
         order by stop_order asc
         """
    }

    var results = ListBuffer[Map[String, Any]]()
    rawQuery(sql, (rs) => {
      var columns = (1 to rs.getMetaData.getColumnCount).map(index =>
          (index -> rs.getMetaData.getColumnName(index))
      ).toMap

      while (rs.next()) {
        results.append(columns.map {
          case(ordinal, name) => (name -> rs.getObject(ordinal))
        })
      }
    })
    results.map(r => r ++ Map(
      "nextArrival" -> new DateTime().plusMinutes(12).toString,
      "arrivals" -> List("11:15", "11:30", "12:00", "12:30")
    )).toList
  }

  def getForRoute(routeId: Long) = getAllStops(Option(routeId)) 

  def getAll() = getAllStops(None)

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

