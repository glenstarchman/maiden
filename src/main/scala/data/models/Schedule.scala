package com.maiden.data.models

import java.sql.Timestamp
import scala.collection.mutable.ArrayBuffer
import org.joda.time._
import MaidenSchema._
import com.maiden.common.exceptions._
import com.maiden.common.Enums._
import com.maiden.common.{Geo, Osrm}

case class Schedule(override var id: Long=0, 
                    var routeId: Long = 0,
                    var stopId: Long = 0,
                    var dayOfWeek: Int = 0,
                    var stopTime: String = "",
                    var masterScheduleId: String  = "",
                    var createdAt: Timestamp=new Timestamp(System.currentTimeMillis),
                    var updatedAt: Timestamp=new Timestamp(System.currentTimeMillis)
) extends BaseMaidenTableWithTimestamps {


}

object Schedule extends CompanionTable[Schedule] {

  implicit def dbl2flt(d: java.lang.Double) = d.toFloat

  val MAX_STOP_TIME = 1440

  def generateSchedule(routeId: Long, days: List[Int], startHour: Int, endHour: Int, startMinute: Int = 0) = {

    //wipe out the existing schedule for this route
    /*withTransaction {
      Schedules.deleteWhere(s => s.routeId === routeId)
    }
    */

    val totalMinutes = (endHour - startHour) * 60
    println("total operating minutes=" + totalMinutes)

    val stops = fetch {
      from(Stops)(s =>
      where(s.routeId === routeId)
      select(s)
      orderBy(s.stopOrder))
    }

    days.foreach(day => {
      var hour = startHour
      //assume all routes start on the hour
      //if (hour <= endHour + 20) {
      var minute = startMinute
      //do the first one
      var tripCounter = 1

      val initialStop = Schedule(routeId = routeId,
                                 stopId= stops(0).id,
                                 dayOfWeek = day,
                                 masterScheduleId = s"${routeId}:${startMinute}:1",
                                 stopTime = s"${"%02d".format(hour)}:${"%02d".format(minute)}")

      withTransaction {
        Schedules.upsert(initialStop)
      }

      while(minute <= totalMinutes) {
        val masterScheduleId = s"${routeId}:${startMinute}:${tripCounter}"
        stops.zipWithIndex.foreach {
          case (stop, index) => {
            val nextStop = if (index < stops.size - 1) {
              stops(index + 1)
            } else {
              stops(0)
            }
            //get the eta between these stops
            val inBetween = Trip.getInBetweenStops(stops, stop, nextStop) ++ List(nextStop)
            val startCoords = Geo.latLngPairFromWKB(stop.geom)
            //.map(x => x("latitude"), x("longitude"))
            val otherCoords = inBetween.map(s => {
              val coords = Geo.latLngFromWKB(s.geom)
              (coords("latitude").toFloat, coords("longitude").toFloat)
            })
            val osrm = Osrm.getRouteAndEta(startCoords, otherCoords)
            val baseEta = osrm("eta").toString.toInt
            //handle busy times (eg, 4 - 6, 8 - 10)
            /*val multiplier = if ((hour >= 16 && hour <= 18) ||
                (hour >= 20 && hour <= 22)) {
                1.25
            } else {
              1.1
            }
            */
            val multiplier = 1

            val eta = Math.round(baseEta * multiplier) + (inBetween.size * 120) //add 2 minutes per stop
            minute += (eta / 60)
            hour = startHour + (minute/60)

            /*if (minute >= 60) {
              println("rolling over")
              println(hour)
              hour += 1
              println(hour)
              minute = minute - 60
            }
            */
            val realMinute =minute % 60

            val modifier = if (hour >= 24) {
              "AM"
            } else {
              "PM"
            }

            val time = if (hour >=24) {
              s"${"%02d".format(hour - 24)}:${"%02d".format(realMinute)}"
            } else {
              s"${hour}:${"%02d".format(realMinute)}"
            }
            //println(s"${nextStop.id}:${nextStop.name}: ${time}")
            val sched = Schedule(routeId = routeId,
              stopId= nextStop.id,
              dayOfWeek = if (hour >= 24) {
                if (day + 1 == 7) {
                  0
                } else {
                  day + 1
                }
              } else {
                day
              },
              masterScheduleId = masterScheduleId,
              stopTime = time)

            withTransaction {
              Schedules.upsert(sched)
            }
          }
        }
        tripCounter += 1
      }
    })
  }

  def getTodaysSchedule(stopId: Long) = {
    val today = new DateTime()
    //for trips beyond midnight
    val tomorrow = today.plusDays(1)

    val todayDay = if (today.getDayOfWeek == 7) {
      0
    } else {
      today.getDayOfWeek
    }

    val tomorrowDay = if (tomorrow.getDayOfWeek == 7) {
      0
    } else {
      tomorrow.getDayOfWeek
    }

    val sched = fetch {
      from(Schedules)(s =>
      where(
        (s.stopId === stopId) and
        (s.dayOfWeek === todayDay or s.dayOfWeek === tomorrowDay)
      )
      select(s)
      orderBy(s.stopTime))
    }

    sched.filter(s => {
      val t = s.stopTime.split(':')
      val stopHour = t(0).toInt
      val stopMinute = t(1).toInt

      val todayStopTime = today
        .withHourOfDay(stopHour)
        .withMinuteOfHour(stopMinute)

      val tomorrowStopTime = tomorrow
        .withHourOfDay(stopHour)
        .withMinuteOfHour(stopMinute)

      (today.isBefore(todayStopTime) && Minutes.minutesBetween(today, todayStopTime).getMinutes <= MAX_STOP_TIME) ||
      (today.isBefore(tomorrowStopTime) && Minutes.minutesBetween(today, tomorrowStopTime).getMinutes <= MAX_STOP_TIME)
    })
    .sortBy(_.dayOfWeek)
    .map(s => Map("day" -> s.dayOfWeek ,  "id" -> s.id, "time" -> s.stopTime))
  }
 
  def getForStop(stopId: Long, routeId: Long) = {

    var route = fetchOne {
      from(Routes)(r => 
      where(r.id === routeId)
      select(r))
    }

    //TODO: need to account for days like "6...3"... 
    route match {
      case Some(r) => {
        var dayStart = r.dayStart
        var stopSchedule = fetch {
          from(Schedules)(s => 
          where(
            (s.stopId === stopId) and
            (s.routeId === routeId) and
            (s.dayOfWeek gte r.dayStart) or 
            (s.dayOfWeek lte r.dayEnd)
          )
          select(s))
        }.groupBy(x => x.dayOfWeek)
        //build out the schedule map
        val days = if (r.dayStart < r.dayEnd) {
          r.dayStart to r.dayEnd
        } else {
          (r.dayStart to 6) ++ (0 to r.dayEnd)
        }

        var sched = days.map(day=> {
          DayOfWeek(day).toString -> {
            if (stopSchedule.contains(day)) {
              stopSchedule(day).map(x => Map("id" ->x.id,  "time" -> x.stopTime))
            } else {
              List()
            }
          }
        }).toMap
        sched
      }
      case _ => throw(new Exception("No such route"))
    }
  }
}
