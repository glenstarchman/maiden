package com.maiden.data.models

import java.sql.Timestamp
import scala.collection.mutable.ListBuffer
import com.maiden.common.Types._
import com.maiden.common.converters.Convertable
import MaidenSchema._
import com.maiden.common.MaidenCache._
import com.maiden.common.exceptions._
import com.maiden.common.Codes._
import com.maiden.common.Enums.StopType._


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
) extends Convertable 

object Stop {

  def getForRoute(routeId: Long): List[Map[String, Any]] = { 
    var sql = s"""
  select id, route_id, stop_order, name, address, description, 
         details, thumbnail,
         ST_X(geom) as latitude,
         ST_Y(geom) as longitude
  from stop
  where route_id = ${routeId}
  order by stop_order asc
  """

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
    results.toList
  }
}

