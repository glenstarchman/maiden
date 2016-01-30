package com.maiden.common

import scala.collection.JavaConversions._
import com.vividsolutions.jts.geom._
import com.vividsolutions.jts.io.{WKBReader, WKBWriter};
import org.opentripplanner.common.geometry.SphericalDistanceLibrary

object Geo {
  implicit def dbl2flt(d: java.lang.Double) = d.toFloat
  implicit def flt2dbl(d: Float) = d.toDouble

  def bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double) = {
    val longitude1 = lon1
    val longitude2 = lon2
    val latitude1 = Math.toRadians(lat1)
    val latitude2 = Math.toRadians(lat2)
    val longDiff = Math.toRadians(longitude2-longitude1)
    val y = Math.sin(longDiff) * Math.cos(latitude2)
    val x = Math.cos(latitude1) * Math.sin(latitude2) - Math.sin(latitude1)* Math.cos(latitude2) * Math.cos(longDiff);
    val resultDegree= (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    val coordNames = List("N","NNE", "NE","ENE","E", "ESE","SE","SSE", "S","SSW", "SW","WSW", "W","WNW", "NW","NNW", "N")
    var directionId = Math.round(resultDegree / 22.5)
    // no of array contain 360/16=22.5
    if (directionId < 0) {
      directionId = directionId + 16;
    }
    (resultDegree, coordNames(directionId))
  }

  def isBeyond(bearing: (Float, String), lat1: Float, lon1: Float, 
               lat2: Float, lon2: Float) = {

    //given a bearing and two sets of coordinates, 
    //determine if lat1/lon1 is BEYOND lat2/lon2
    val bearingDegrees = bearing._1
    val bearingName = bearing._2 
    
    //val coordNames = List("N","NNE", "NE","ENE","E", "ESE","SE","SSE", "S","SSW", "SW","WSW", "W","WNW", "NW","NNW", "N")
    bearingName match {
      case "W" => {
        //calc longitude
        if (lon1 < lon2) true
        else false
      }

      case "E" => {
        if (lon1 > lon2) true
        else false
      }
    }
  }

  lazy val gm = new GeometryFactory(new PrecisionModel(), 4326)

  def hex2Bytes(hex: String): Array[Byte] = {
    hex
      .replaceAll("[^0-9A-Fa-f]", "")
      .sliding(2, 2)
      .toArray.
      map(Integer.parseInt(_, 16).toByte)
  }

  def bytes2Hex(bytes: Array[Byte], sep: Option[String] = None): String = {
    sep match {
      case None => bytes.map("%02x".format(_)).mkString
      case _ => bytes.map("%02x".format(_)).mkString(sep.get)
     }
  }

  def latLngFromWKB(wkb: String) = {
    val wk = new WKBReader(gm)
    val geom = wk.read(hex2Bytes(wkb)).getCoordinate
    Map("latitude" -> geom.y, "longitude" -> geom.x)
  }

  def latLngListFromWKB(wkb: String) = {
    val wk = new WKBReader(gm)
    val geom = wk.read(hex2Bytes(wkb)).getCoordinates
    geom.map(g => 
      Map("latitude" -> g.y, "longitude" -> g.x)
    ).toList
  }

  def latLngListToWKB(coords: List[List[Float]]) = {
    val points = coords.map(c => 
        makePoint(
          c(0).toString.toDouble, 
          c(1).toString.toDouble)
          .getCoordinate)
      .toArray
    val route = gm.createLineString(points)
    val w = new WKBWriter()
    WKBWriter.bytesToHex(w.write(route))
  }


  def latLngToWKB(coords: List[Float]) = latLngListToWKB(List(coords))

  def makePoint(lat: Double, lng: Double) = gm.createPoint(new Coordinate(lat,lng))

  def generateBoundingBox(lat: Float, lng: Float, 
      latDistance: Int, lonDistance: Int) = {

    val mPerDegreeLat = 111111.111111;
    val lonScale = Math.cos(Math.PI * lat / 180);
    val latExpand = latDistance / mPerDegreeLat;
    val lonExpand = (lonDistance / mPerDegreeLat) / lonScale;
    val point = makePoint(lat, lng)
    val envelope = point.getEnvelopeInternal()   
    envelope.expandBy(
      lonExpand, 
      latExpand
    )
    val geometry = gm.toGeometry(envelope)
    geometry.getCoordinates().map(geom =>
      List(geom.x, geom.y)
    ).toList
  }
}
