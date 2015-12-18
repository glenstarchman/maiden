package com.maiden.data.models


import java.sql.Timestamp
import com.maiden.common.Types._
import MaidenSchema._
import com.maiden.common.MaidenCache._
import com.maiden.data.ConnectionPool
import com.maiden.common.exceptions._
import com.maiden.common.Codes._
import com.maiden.common.Enums.FareType._

case class Fare(override var id: Long=0, 
                var fareType: FareType = Fixed,

                var createdAt: Timestamp=new Timestamp(System.currentTimeMillis), 
                var updatedAt: Timestamp=new Timestamp(System.currentTimeMillis)) 
  extends BaseMaidenTableWithTimestamps {

} 
