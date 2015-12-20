package com.maiden.common

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.WKBReader;


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


}



