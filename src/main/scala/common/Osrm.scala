package com.maiden.common

import org.joda.time._
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
      s"loc=${lat},${lon}"
     }.mkString("&") + s"&loc=${locs(0)._1},${locs(0)._2}"

     val url = s"${endpoint}?${locParams}&compression=false"
     val http = new HttpClient(url)
     val p = http.fetchAsMap()
     println(url)
     val geom = p("route_geometry").asInstanceOf[List[List[(Float, Float)]]]
     geom ++ List(geom(0))
  }

  def getRouteUnenclosed(locs: List[(Float, Float)]) = {
    val endpoint = buildEndpoint("viaroute")
    val locParams = locs.map { case (lat, lon) => 
      s"loc=${lat},${lon}"
     }.mkString("&") + s"&loc=${locs(0)._1},${locs(0)._2}"

     val url = s"${endpoint}?${locParams}&compression=false"
     val http = new HttpClient(url)
     val p = http.fetchAsMap()
     val geom = p("route_geometry").asInstanceOf[List[List[(Float, Float)]]]
     geom 
  }
  //this is where we can algorithmically add/subtract 
  //from the ETA based on time of day, etc...
  private[this] def fixupTime(d: DateTime) = {

  }

  def getDistanceTable(start: (Float, Float), points: List[(Float,Float)]) = {
    val endpoint = buildEndpoint("table")
    val locParams = points.map { case (lat, lon) => 
      s"loc=${lat},${lon}"
     }.mkString("&") 
     val url = s"${endpoint}?loc=${start._1},${start._2}&${locParams}"
     println(url)
     val http = new HttpClient(url)
     val p = http.fetchAsMap()
     p("distance_table").asInstanceOf[List[List[BigInt]]]
  }


  def getRouteAndEta(start: (Float, Float), points: List[(Float, Float)]) = {
    val endpoint = buildEndpoint("viaroute")
    val locParams = points.map { case (lat, lon) => 
      s"loc=${lat},${lon}"
     }.mkString("&") 
     val url = s"${endpoint}?loc=${start._1},${start._2}&${locParams}&compression=false"
     val http = new HttpClient(url)
     println(url)
     val p = http.fetchAsMap()
     val geometry = p("route_geometry")
     val summary = p("route_summary").asInstanceOf[Map[String, Any]]
     Map(
       "geometry" -> geometry,
       "distance" -> summary("total_distance"),
       "eta" -> summary("total_time")
     )
  }

}


case class OsrmInstruction(
  direction: String

)



