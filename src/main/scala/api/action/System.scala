package com.maiden.api.action

import xitrum.annotation.{GET, POST, First, Swagger}
import xitrum.SkipCsrfCheck
import com.maiden.data.models._
import com.maiden.data.models.MaidenSchema._
import com.maiden.common.converters.ListConverter
import com.maiden.common.MaidenConfigFactory.{env, config}
import com.maiden.common.social.MaidenFacebook
import com.maiden.common.exceptions._
import com.maiden.common.Codes.StatusCode
import com.maiden.common.Enums._


@Swagger(
  Swagger.Tags("System", "No Auth"),
  Swagger.Produces("application/json")
)
trait SystemApi extends JsonAction 

@Swagger(
  Swagger.Tags("System", "Needs Authentication"),
  Swagger.Produces("application/json"),
  Swagger.StringHeader("X-MAIDEN-AT", "Access Token")
)
trait AuthorizedSystemApi extends AuthorizedJsonAction


@GET("api/system/configuration")
@POST("api/system/configuration")
@Swagger(
  Swagger.OperationId("get_system_constants"),
  Swagger.Summary("get a list of all system variables")
)
class SystemVariables extends SystemApi {
  def execute() {
    futureExecute(() => {
      val data = Map(
        "app.mode" -> env, 
        "fb.api_key" -> config("fb.api_key"),
        "fb.app_id" -> config("fb.app_id"),
        "cdn.uri" -> config("cdn.uri"),
        "osrm.server" -> config("osrm.server"),
        "osrm.port" -> config("osrm.port"),
        "pubnub.publish_key" -> config("pubnub.publish_key"),
        "pubnub.subscribe_key" -> config("pubnub.subscribe_key"),
        "raygun.api_key" -> config("raygun.api_key"),
        "host.url" -> config("host.url"),
        "stripe.pub_key" -> config("stripe.pub_key")
      ) 
      (R.OK, data)
    })
  }
}
