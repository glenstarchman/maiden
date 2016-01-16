package com.maiden.common

import java.util.HashMap
import java.util.Map
import scala.collection.immutable.{Map => SMap}
import scala.collection.JavaConversions._
import com.stripe.Stripe
import com.stripe.exception.StripeException
import com.stripe.model._
import com.stripe.net.RequestOptions._
import org.json4s._
import org.json4s.native.JsonMethods._
import com.maiden.common.MaidenConfigFactory.{env, config}
import com.maiden.data.models.{Stripe => StripeModel}

class StripeHelper {

  implicit val formats = DefaultFormats
  lazy val secretKey = config("stripe.secret_key").toString
  lazy val pubKey = config("stripe.pub_key").toString

  private[this] lazy val options = (new RequestOptionsBuilder())
    .setApiKey(secretKey).build()


  Stripe.apiKey = secretKey

  def createCustomer(userId: Long, token: String, description: String, last4: String = "", brand: String = "", isDefault: Boolean = true) = {
    val custMap = new HashMap[String, Object]()
    custMap.put("source", token)
    custMap.put("description", description) 
    val stripeCustomer = Customer.create(custMap)
    StripeModel.create(userId, stripeCustomer.getId, description, 
                       last4, brand, isDefault)
  }

  def charge(stripeCustomer: String, amount: Int, 
             metadata: SMap[String, String] = SMap.empty) = {

    val chargeMap: Map[String, Object] = new HashMap[String, Object]()
    chargeMap.put("amount", amount.toString)
    chargeMap.put("currency", "usd")
    val cardMap: Map[String, Object] = new HashMap[String, Object]()
    chargeMap.put("customer", stripeCustomer)
    chargeMap.put("metadata", mapAsJavaMap(metadata))
    try {
        val charge = Charge.create(chargeMap, options) 
        println(charge)
    } catch {
      case e: Exception => e.printStackTrace
    }
  }
}
