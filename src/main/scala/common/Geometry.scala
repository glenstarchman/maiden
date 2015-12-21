package com.maiden.common

import scala.collection.JavaConversions._
import com.vividsolutions.jts.geom._
import com.vividsolutions.jts.io.{WKBReader, WKBWriter};
import org.opentripplanner.common.geometry.SphericalDistanceLibrary


object Geo {

  lazy val gm = new GeometryFactory(new PrecisionModel(), 4326)

  def hex2Bytes(hex: String): Array[Byte] = {
    hex.replaceAll("[^0-9A-Fa-f]", "").sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)
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

  def latLngToWKB(coords: List[List[Float]]) = {
    val points = coords.map(c => 
        makePoint(c(0), c(1))
          .getCoordinate)
      .toArray
    val route = gm.createLineString(points)
    val w = new WKBWriter()
    WKBWriter.bytesToHex(w.write(route))

  }

  def makePoint(lat: Float, lng: Float) = gm.createPoint(new Coordinate(lat,lng))

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



