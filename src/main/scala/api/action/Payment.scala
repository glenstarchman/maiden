package com.maiden.api.action

import xitrum.annotation.{GET, POST, First, Swagger}
import com.maiden.common.StripeHelper
import com.maiden.data.models.Stripe
import com.maiden.common.exceptions._
import com.maiden.common.Codes.StatusCode
import com.maiden.common.Enums._

@Swagger(
  Swagger.Tags("Payment", "Needs Authentication"),
  Swagger.Produces("application/json"),
  Swagger.StringHeader("X-MAIDEN-AT", "Access Token")
)
trait AuthorizedPaymentApi extends AuthorizedJsonAction

@POST("api/payment/add")
@GET("api/payment/add")
@Swagger(
  Swagger.OperationId("add_payment"),
  Swagger.Summary("add a payment method"),
  Swagger.StringQuery("token", "Stripe access token"),
  Swagger.StringQuery("description", "card description"),
  Swagger.OptStringQuery("default", "default card [true|false]"),
  Swagger.OptStringQuery("brand", "the card's brand"),
  Swagger.OptStringQuery("last4", "last 4 card digits")
)
class AddPayment extends AuthorizedPaymentApi {
  def execute() {
    futureExecute(() => { 
      val s = new StripeHelper()
      val userId = user.get.id
      val token = param[String]("token")
      val description = param[String]("description")
      val isDefault = paramo("default") match {
        case Some(x) if x == "true" => true
        case _ => false
      }

      val last4 = paramo("last4") match {
        case Some(x) => x
        case _ => ""
      }

      val brand = paramo("brand") match {
        case Some(x) => x
        case _ => ""
      }
      val r = s.createCustomer(userId, token, description, last4, brand, isDefault)
      (R.OK, r.asMap)
    })
  }
}

@POST("api/payment/list")
@GET("api/payment/list")
@Swagger(
  Swagger.OperationId("list_payment_methods"),
  Swagger.Summary("lists all payment method")
)
class ListPaymentMethods extends AuthorizedPaymentApi {
  def execute() {
    futureExecute(() => { 
      val cards = Stripe.getForUser(user.get.id)
      val r = Map(
        "cards" -> cards.map(_.asMap)
      )
      (R.OK, r)
    })
  }

}
