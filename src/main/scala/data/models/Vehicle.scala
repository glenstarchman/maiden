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


case class Vehicle(override var id: Long=0, 
                var driverId: Long = 0,
                var routeId: Long = 0,
                var maximumOccupancy: Int  = 0,
                var license: String = "",
                var color: String = "",
                var model: String = "",
                var active: Boolean = false,
                var currentLocation: Long = 0,
                var thumbnail: String = "",
                var createdAt: Timestamp=new Timestamp(System.currentTimeMillis), 
                var updatedAt: Timestamp=new Timestamp(System.currentTimeMillis)) 
  extends BaseMaidenTableWithTimestamps {


  override def extraMap() = Map(
    "thumbnail" -> {
      thumbnail match {
        case x: String => x
        case _ => "http://www.autoblog.com/used-img/defaultImage.png?v=release%2F4.25-195915"
      }
    },
    "driver" -> {
      User.get(driverId) match {
        case Some(u) => Map(
          "name" -> u.profile.get.firstName,
          "thumbnail" -> {
            u.profile match {
              case Some(p) => p.profilePicture
              case _ => "https://diasp.eu/assets/user/default.png"
            }
          }
        )
        case _ => Map.empty
      }
    },
    "currentLocation" -> {
      GpsLocation.getCurrentForUser(driverId) match {
        case Some(c) => List(c.latitude, c.longitude)
        case _ => List(0f, 0f)
      }
    },
    "nextStop" -> Map(
      "id"-> 2, //fill in later
      "name" -> "Olaf's",
      "eta" -> new DateTime().plusMinutes(10).toString //eta as a datetime to the next stop
    ),
    "currentlyPlaying" -> Map(
      "name" -> "Slayer - Seasons in the Abyss", 
      "thumbnail" -> "https://upload.wikimedia.org/wikipedia/en/1/1b/Slayer_-_Seasons_in_the_Abyss.jpg"
    ),
    "currentPassengers" -> 6 //fill in later
  )

  def miniMap() = Map(
    "id" -> id,
    "currentLocation" -> {
      GpsLocation.getCurrentForUser(driverId) match {
        case Some(c) => List(c.latitude, c.longitude)
        case _ => List(0f, 0f)
      }
    },
    "nextStop" -> Map(
      "id"-> 2, //fill in later
      "name" -> "Olaf's",
      "eta" -> new DateTime().plusMinutes(10).toString //eta as a datetime to the next stop
    ),
    "currentlyPlaying" -> Map(
      "name" -> "Slayer - Seasons in the Abyss", 
      "thumbnail" -> "https://upload.wikimedia.org/wikipedia/en/1/1b/Slayer_-_Seasons_in_the_Abyss.jpg"
    ),
    "currentPassengers" -> 6 //fill in later
  )

}

object Vehicle extends CompanionTable[Vehicle] {


  def getForRoute(routeId: Long) = {
    val vehicles = fetch {
      from(Vehicles)(v => 
      where(v.routeId === routeId and v.active === true)
      select(v))
    }
    vehicles.par.map(v => v.asMap).toList
  }

  def getMinimalForRoute(routeId: Long) = {
    val vehicles = fetch {
      from(Vehicles)(v => 
      where(v.routeId === routeId and v.active === true)
      select(v))
    }
    vehicles.par.map(v => v.miniMap).toList
  }


}


