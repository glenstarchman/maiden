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


}

object Vehicle extends CompanionTable[Vehicle] {

}


