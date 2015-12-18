
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


case class Route(override var id: Long=0, 
                var name: String = "",
                var description: String = "",
                var detail: String = "",
                var hourStart: String = "00:00",
                var hourEnd: String = "00:00",
                var dayStart: Int = 0,
                var dayEnd: Int = 0,
                var active: Boolean = true,
                var thumbnail: String = "",
                var createdAt: Timestamp=new Timestamp(System.currentTimeMillis), 
                var updatedAt: Timestamp=new Timestamp(System.currentTimeMillis)) 
  extends FriendlyIdable with BaseMaidenTableWithTimestamps {


}

object Route extends CompanionTable[Route] {

  def getAllActiveRoutes() = fetch {
    from(Routes)(r => 
    where(r.active === true)
    select(r)
  }

  def getCurrentlyActive() = {
    val now = new DateTime(System.currentTimeMillis);
    val nowDay = now.getDayOfWeek
    val nowHour = now.getHourOfDay
    val nowMinute = now.getMinuteOfHour
    var min = if (nowMinute.toString.length < 2) {
      s"0${nowMinute}"
    } else {
      nowMinute.toString
    }
    var hour = if (nowHour.toString.length < 2) {
      s"0${nowHour}"
    } else {
      nowHour.toString
    }
    val nowTime = s"${hour}:${min}"

    fetch {
      from(Routes(r => 
      where (r.

    }
  }
}
