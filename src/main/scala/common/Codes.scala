/*
 * Copyright (c) 2015. Maiden, Inc All Rights Reserved
 */

package com.maiden.common

import scala.util.parsing.json.{JSONArray, JSONObject}
/* model status codes */

object Codes {


  sealed abstract class StatusCode(val code: Int, val name: String,
         var description: String="", var details: String = "") extends Ordered[StatusCode] {

    def compare(that: StatusCode) = this.code - that.code

    override def toString = name

    def asMap() = Map[String, Any](
      "code" -> this.code,
      "name" -> this.name,
      "description" -> this.description
    )

    def asJson() = JSONObject(this.asMap)

  }

  /* Generic codes */
  case object OK extends StatusCode(1, "OK", "The operation succeeded")
  case object GENERIC_ERROR extends StatusCode(-1, "GenericFailure", "The operation failed.")
  case object ROUTE_NOT_FOUND extends StatusCode(-2, "ROUTE_NOT_FOUND")
  case object INTERNAL_SERVER_ERROR extends StatusCode(-3, "INTERNAL_SERVER_ERROR")
  case object ENTITY_NOT_FOUND extends StatusCode(-4, "ENTITY_NOT_FOUND")

  /* Model specific codes */

  /* USER = 100 */
  case object USER_CREATED extends StatusCode(100, "USER_CREATED")
  case object USER_UPDATED extends StatusCode(101, "USER_UPDATED")
  case object USER_LOGIN extends StatusCode(102, "USER_LOGIN")
  case object USER_CREATION_FAILED extends StatusCode(-101, "USER_CREATION_FAILED")
  case object USER_ALREADY_EXISTS extends StatusCode(-102, "USER_ALREADY_EXISTS")
  case object INVALID_EMAIL extends StatusCode(-103, "INVALID_EMAIL_ADRRESS")
  case object INVALID_PASSWORD extends StatusCode(-104, "INVALID_PASSWORD", "Passwords must be between 8 and 16 characters in length")
  case object INVALID_USERNAME extends StatusCode(-105, "INVALID_USERNAME", "Username must be between 3 and 24 characters in length, and must not contain spaces or other special characters")
  case object INVALID_ACCESS_TOKEN extends StatusCode(-106, "INVALID_STATUS_CODE", "The request was made with an invalid access token")
  case object ACCESS_TOKEN_EXPIRED extends StatusCode(-107, "ACCESS_TOKEN_EXPIRED", "The request was made with an expired access token")
  case object INVALID_LOGIN_CREDENTIALS extends StatusCode(-108, "INVALID_LOGIN_CREDENTIALS", "Either the username or the password is incorrect")
  case object USER_NOT_FOUND extends StatusCode(-109, "USER_NOT_FOUND")
  case object UNAUTHORIZED extends StatusCode(-110, "UNAUTHORIZED", "Either you are not logged in, or your access token is invalid")
  case object ADMIN_ONLY extends StatusCode(-110, "ADMIN_ONLY", "This resource requires administrative permissions")
  case object INVALID_SOCIAL_CREDENTIALS extends StatusCode(-111, "INVALID_SOCIAL_CREDENTIALS", "The token supplied for the given provider is invalid")
  case object RECIPIENT_NOT_CONTACTABLE extends StatusCode(-112, "RECIPIENT_NOT_CONTACTABLE", "The specified recipient has elected to not receive communications from Maiden")

  case object MISSING_SOCIAL_ACCESS_TOKEN extends StatusCode(-112, "MISSING_SOCIAL_ACCESS_TOKEN", "You did not supply an access token when attempting to login via a social service")

  case object USER_ACCOUNT_MISSING_IDENTITY extends StatusCode(-113, "USER_ACCOUNT_MISSING_IDENTITY", "The user account has no associated identity account")

  case object PASSWORD_RESET_CODE_INVALID extends StatusCode(-114, "PASSWORD_RESET_CODE_INVALID", "The password reset code is invalid")

  /* ROLE = 200 */
  case object ROLE_CREATED extends StatusCode(200, "ROLE_CREATED")
  case object ROLE_UPDATED extends StatusCode(201, "ROLE_UPDATED")
  case object ROLE_CREATION_FAILED extends StatusCode(-201, "ROLE_CREATION_FAILED")

  /* PROFILE = 300 */
  case object PRIVATE_PROFILE extends StatusCode(-301, "PRIVATE_PROFILE", "The profile is marked as private")


  /* PROJECT = 400 */

  case object PROJECT_NOT_FOUND extends StatusCode(-401, "PROJECT_NOT_FOUND", "The specified project could not be found")

  case object PROJECT_DATA_MISSING extends StatusCode(-402, "PROJECT_DATA_MISSING", "The project could not be created because data is missing")

  case object PROJECT_USER_UNAUTHORIZED extends StatusCode(-403, "PROJECT_USER_UNAUTHORIZED", "The user is not authorized to perform the given action on the given project")

  case object PROJECT_UNKNOWN_IMAGE_TYPE extends StatusCode(-404, "PROJECT_UNKNOWN_IMAGE_TYPE", "The uploaded file is of an unknown type")
  case object PROJECT_FILE_UPLOAD_FAILED extends StatusCode(-405, "PROJECT_FILE_UPLOAD_FAILED", "The was an issue with the file upload")
  case object PROJECT_INVITE_CODE_INVALID extends StatusCode(-406, "PROJECT_INVITE_CODE_INVALID", "The invite code is invalid")
  case object PROJECT_INVITE_CODE_MISSING extends StatusCode(-407, "PROJECT_INVITE_CODE_MISSING", "The invite code is missing")
  case object PROJECT_TYPE_INVALID extends StatusCode(-408, "PROJECT_TYPE_INVALID", "The type of the project is invalid")
  

  /* CONTRIBUTER REQUEST = 500 */
  case object NO_VALID_REQUESTEES extends StatusCode(-501, "NO_VALID_REQUESTEES", "The contributor request did not contain any valid requestees")

  import com.maiden.macros.EnumerationMacros._
  val statusCodes: Set[StatusCode] = sealedInstancesOf[StatusCode]
  val statusCodesAsJson: JSONArray = 
    JSONArray(statusCodes.toList.sortBy(_.code).map(_.asJson()))


  /* TEAM/CONTRIBUTION = 500 */
  case object CONTRIBUTION_CREATED extends StatusCode(500, "CONTRIBUTION_CREATED")
  case object CONTRIBUTION_DATA_MISSING extends StatusCode(-501, "CONTRIBUTION_DATA_MISSING")
  case object TEAM_CREATED extends StatusCode(501, "TEAM_CREATED")
  case object TEAM_MISSING_DATA extends StatusCode(-501, "TEAM_MISSING_DATA")
  case object TEAM_MEMBER_INVALID_DATA extends StatusCode(-502, "TEAM_MEMBER_INVALID_DATA")


  /* GENERAL FAILURES = -600 */
  case object S3_UPLOAD_TIMED_OUT extends StatusCode(-600, "S3_UPLOAD_TIMED_OUT", "The upload to S3 timed out")
  case object OEMBED_TIMED_OUT extends StatusCode(-601, "OEMBED_TIMED_OUT", "Attempt to get Oembed information timed out")
  case object INVALID_OEMBED_FORMAT extends StatusCode(-602, "INVALID_OEMBED_FORMAT", "The format of the returned OEMBED was inavlid")
  case object INVALID_OEMBED_PROVIDER extends StatusCode(-603, "INVALID_OEMBED_PROVIDER", "The oembed provider is invalid")
  case object FACEBOOK_FAILURE extends StatusCode(-604, "FACEBOOK_FAILURE", "Unable to retrieve information from Facebook")
  case object MANDRILL_FAILURE extends StatusCode(-605, "MANDRILL_FAILURE", "Unable to send email via Mandrill")

  case object CREATE_OR_UPDATE_FAILED extends StatusCode(-606, "CREATE_OR_UPDATE_FAILED", "The create or update operation failed")

  case object MISSING_PARAMETER extends StatusCode(-607, "MISSING_PARAMETER", "A required parameter is missing")

}
