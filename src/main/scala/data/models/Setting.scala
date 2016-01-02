package com.maiden.data.models


import java.sql.Timestamp
import com.maiden.common.Types._
import MaidenSchema._
import com.maiden.common.MaidenCache._
import com.maiden.data.ConnectionPool
import com.maiden.common.exceptions._
import com.maiden.common.Codes._
import com.maiden.common.Enums._

case class Setting(override var id: Long=0, 
                var userId: Long = 0,
                var name: String = "",
                var value: String = "",
                var createdAt: Timestamp=new Timestamp(System.currentTimeMillis), 
                var updatedAt: Timestamp=new Timestamp(System.currentTimeMillis)) 
  extends BaseMaidenTableWithTimestamps {



}

object Setting extends CompanionTable[Setting] {

  def getSettingForUser(userId: Long, name: String) = fetchOne {
    from(Settings)(s => 
    where(s.userId === userId and s.name === name)
    select(s))
  }

  def getForUser(userId: Long) = fetch {
    from(Settings)(s =>
    where(s.userId === userId)
    select(s))
  }

  def createOrUpdate(userId: Long, name: String, value: String) = {
    getSettingForUser(userId, name) match {
      //update
      case Some(s) => {
        s.value = value
        withTransaction {
          Settings.upsert(s)
          s
        }
      }
      case _ => {
        val s = Setting(userId = userId, name = name, value = value)
        withTransaction {
          Settings.insert(s)
          s
        }
      }
    }
  }



}
