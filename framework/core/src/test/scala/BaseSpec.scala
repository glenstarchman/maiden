package com.didd.test

import org.scalatest._
import com.didd.data.models._

class BaseSpec extends FlatSpec with Matchers {
  var testUser: User = null
  var testFBAccessToken: String = null
  var testAccessToken: String = null
  var testProject: Project = null
  var testTeam: Team = null
  var testRole: Role = null
}
