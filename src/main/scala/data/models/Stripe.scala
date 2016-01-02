package com.maiden.data.models

import java.sql.Timestamp
import scala.util.parsing.json.{JSON, JSONObject, JSONArray}
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.Extraction._
import org.json4s.native.Serialization._
import org.json4s.native.Serialization
import com.maiden.common.Types._
import MaidenSchema._

case class Stripe(override var id: Long=0, 
                  var userId: Long = 0,
                  var stripeCustomer: String = "",
                  var description: String = "",
                  var last4: String = "",
                  var brand: String = "",
                  var isDefault: Boolean = true,
                  var createdAt: Timestamp=new Timestamp(System.currentTimeMillis), 
                  var updatedAt: Timestamp=new Timestamp(System.currentTimeMillis) 
) extends BaseMaidenTableWithTimestamps {

  //don't pass customer token back to client!
  override def asMap() = Map(
    "description" -> description,
    "last4" -> last4,
    "brand"-> brand,
    "idDefault" -> isDefault,
    "createdAt" -> createdAt,
    "updatedAt" -> updatedAt
  )
}

object Stripe extends CompanionTable[Stripe] {

  def getForUser(userId: Long) = fetch {
    from(Stripes)(s => 
    where(s.userId === userId)
    select(s)
    orderBy(s.createdAt.desc))
  }

  def create(userId: Long, stripeCustomer: String, description: String = "",
             last4: String = "", brand: String = "", 
             isDefault: Boolean = true) = {
    val s = Stripe(userId = userId, stripeCustomer = stripeCustomer, 
                   description = description, last4=last4, brand = brand, 
                   isDefault = isDefault)
    withTransaction {
      Stripes.upsert(s)
      s
    }
  }

}
