/*
 * Copyright (c) 2015. Maiden, Inc All Rights Reserved
 */

/**
 * Created by glen on 9/24/15.
 */

package com.maiden.test

import scala.util.Properties
import org.scalatest._
import com.maiden.data.models._
import com.maiden.data.models.MaidenSchema._
import com.maiden.data.Migrate
import com.maiden.common.social.MaidenFacebook
import com.maiden.common.scraper.Scraper
import com.maiden.common.HttpClient
import com.maiden.common.MaidenConfigFactory
import com.maiden.api.Boot

class MaidenSpec extends BaseSpec with BeforeAndAfterAll {

  def beforeAll(c: Map[String, Any]) {
    Migrate.rebuild
  }

  /* some implicits to make things easier */
  implicit def any2mapany(a: Any) = a.asInstanceOf[Map[String, Any]]
  implicit def any2string(a: Any) = a.toString
  implicit def any2int(a: Any) = a.toString.toInt
  implicit def any2long(a: Any) = a.toString.toLong
  //implicit def mapany2mapseq(a: Map[String, Any]) = a.map { case (k,v) => (k -> Seq(v.toString)) }.toMap


  val env = MaidenConfigFactory.env
  val config = MaidenConfigFactory.config

  val baseUrl = s"${config("host.url")}/api"


  private[this] def request(endpoint: String, method: String = "POST", data: Map[String, Seq[String]] = Map.empty) = {

    val client = new HttpClient(s"${baseUrl}/${endpoint}", method = method, data = data)
    val m = client.fetchAsMap
    m("result")
  }

  /*before {
    Migrate.rebuild
  }

  after {
    Migrate.rebuild
  }
  */

  "Create new user with FB Token" should "return a valid user" in {
    testFBAccessToken = MaidenFacebook.createTestUser()
    val sa = SocialAccount.create("facebook", testFBAccessToken).get
    sa.accessToken should be (testFBAccessToken)
    testUser = sa.user.get
    testAccessToken = testUser.accessToken
    testUser.id should be > 0l
  }

  "Get new user" should "work" in {
    val d = request(s"user/${testUser.id}", method="POST")//${testUser.id}")
    d("id") should be (testUser.id)
  }

  "Update profile data" should "work" in {
    val data = Map( 
      "at" -> Seq(testAccessToken),
      "firstName" -> Seq("MaidenTest"),
      "lastName" -> Seq("User")
    )
    val d = request("user/profile/update", data = data)
    d("profile")("firstName") should be ("MaidenTest")
  }

  "Create user with Identity" should "return a valid user" in {
    val data = Map(
      "username" -> Seq("john"),
      "password" -> Seq("123455667"),
      "email" -> Seq("john@smith100.com"),
      "firstName" -> Seq("John"),
      "lastName" -> Seq("Smith")
    )
    val d = request("user/account/create", data = data)
    d("userName") should be ("john")


  }

}
