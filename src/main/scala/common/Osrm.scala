package com.maiden.common

import com.maiden.common.MaidenConfigFactory.{config, env}

object Osrm {
  lazy val baseUrl = s"${config("osrm.server")}:${config("osrm.port")}"

  private[this] def buildEndpoint(endpoint: String) = s"${baseUrl}/${endpoint}"

  private[this] def request(url: String) = {
    val http = new HttpClient(url)
    http.fetchAsMap
  }

  def getRoute(locs: List[(Float, Float)]) = {
    val endpoint = buildEndpoint("viaroute")
    val locParams = locs.map { case (lat, lon) => 
      s"loc=${lon},${lat}"
     }.mkString("&") + s"&loc=${locs(0)._2},${locs(0)._1}"

     val url = s"${endpoint}?${locParams}&compression=false"
     val http = new HttpClient(url)
     val p = http.fetchAsMap()
     val geom = p("route_geometry").asInstanceOf[List[List[(Float, Float)]]]
     geom ++ List(geom(0))
  }

  private[this] def fixupTime(value: String) = {

  }

  def getDistanceTable(start: (Float, Float), points: List[(Float,Float)]) = {
    val endpoint = buildEndpoint("table")
    val locParams = points.map { case (lat, lon) => 
      s"loc=${lon},${lat}"
     }.mkString("&") 
     val url = s"${endpoint}?loc=${start._1},${start._2}&${locParams}"
     val http = new HttpClient(url)
     val p = http.fetchAsMap()
     p("distance_table").asInstanceOf[List[List[BigInt]]]
  }

}


case class OsrmInstruction(
  direction: String

)



