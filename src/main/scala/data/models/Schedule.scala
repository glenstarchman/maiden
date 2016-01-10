package com.maiden.data.models

import java.sql.Timestamp
import scala.collection.mutable.ArrayBuffer
import org.joda.time._
import MaidenSchema._
import com.maiden.common.exceptions._
import com.maiden.common.Enums._

case class Schedule(override var id: Long=0, 
            var routeId: Long = 0,
            var stopId: Long = 0,
            var dayOfWeek: Int = 0,
            var stopTime: String = "",
            var createdAt: Timestamp=new Timestamp(System.currentTimeMillis), 
            var updatedAt: Timestamp=new Timestamp(System.currentTimeMillis) 
) extends BaseMaidenTableWithTimestamps {


}

object Schedule extends CompanionTable[Schedule] {

  val MAX_STOP_TIME = 1440
  //only return times in the future
  def getTodaysSchedule(stopId: Long, routeId: Long) = {
    val schedule = getForStop(stopId, routeId)
    val today = new DateTime()
    //for trips beyond midnight
    val tomorrow = today.plusDays(1)

    val validSchedules = schedule.filter{ case(k,v) => { 

      val jodaDOWToday = today.getDayOfWeek()

      val jodaDOWTomorrow = if(tomorrow.getDayOfWeek() == 7) {
        0 
      } else {
        tomorrow.getDayOfWeek() - 1
      }

      //println(jodaDOWToday)
      //println(jodaDOWTomorrow)
      ((DayOfWeek.withName(k).id == jodaDOWToday) || 
       (DayOfWeek.withName(k).id == jodaDOWTomorrow))
    }}

    var ret = new ArrayBuffer[Map[String, Any]]
    //println(validSchedules)

    validSchedules.zipWithIndex.foreach { case ((k,v),i)  => {
      val valid = v.filter(s => {
        val t = s("time").toString.split(':')
        val stopHour = t(0).toInt
        val stopMinute = t(1).toInt

        val todayStopTime = today
                            .withHourOfDay(stopHour)
                            .withMinuteOfHour(stopMinute)

        val tomorrowStopTime = tomorrow
                               .withHourOfDay(stopHour)
                               .withMinuteOfHour(stopMinute)

        if (i == 0) {
          (today.isBefore(todayStopTime) && Minutes.minutesBetween(today, todayStopTime).getMinutes <= MAX_STOP_TIME)
        } else {
          (today.isBefore(tomorrowStopTime) && Minutes.minutesBetween(today, tomorrowStopTime).getMinutes <= MAX_STOP_TIME)
        }
    }).map(s => Map("day" -> DayOfWeek.withName(k).id,  "id" -> s("id"), "time" -> s("time")))
      if (valid.size > 0) {
        valid.foreach(x => ret += x) 
      }
    }}
    ret.toList
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
