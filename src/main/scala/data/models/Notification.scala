package com.maiden.data.models

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import java.sql.Timestamp
import scala.util.parsing.json.{JSON, JSONObject, JSONArray}
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.Extraction._
import org.json4s.native.Serialization._
import org.json4s.native.Serialization
import com.maiden.common.Types._
import MaidenSchema._
import com.maiden.common.PushNotification

case class Notification(override var id: Long=0, 
                var userId: Long = 0,
                var token: String = "",
                var deviceType: String = "",
                var createdAt: Timestamp=new Timestamp(System.currentTimeMillis), 
                var updatedAt: Timestamp=new Timestamp(System.currentTimeMillis) 
) extends BaseMaidenTableWithTimestamps {


}

object Notification extends CompanionTable[Notification] {


  def exists(userId: Long, token: String) = fetchOne {
    from(Notifications)(n => 
    where(n.userId === userId and n.token === token)
    select(n))
  }

  def create(userId: Long, token: String, deviceType: String) = {
    exists(userId, token) match {
      //do not create if it exists
      case Some(x) => x
      case _ => {
        val n = Notification(userId = userId, token = token, deviceType = deviceType)

        withTransaction {
          Notifications.upsert(n)
          n
        }
      }
    }
  }

  //returns all tokens for a user as a List 
  def getTokensForUser(userId: Long) = fetch {
    from(Notifications)(n => 
    where(n.userId === userId)
    select(n.token)
    orderBy(n.createdAt.desc))
  }

  def removeForUser(userId: Long) = withTransaction {
    Notifications.deleteWhere(n => n.userId === userId)
  }

  def removeByToken(token: String) = withTransaction {
    Notifications.deleteWhere(n => n.token === token)
  }
  
  //send a push notification to all of the user's registered devices
  def send(userId: Long, message: String, isProduction: Boolean = false) {
    Future {
      PushNotification.send(getTokensForUser(userId), message, isProduction)
    }
  }


}
