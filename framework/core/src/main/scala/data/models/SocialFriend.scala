/*
 * Copyright (c) 2015. Didd, Inc All Rights Reserved
 */

package com.maiden.framework.data.models

import java.sql.Timestamp
import com.maiden.framework.common.Types._
import DiddSchema._


case class SocialFriend(var id: Long = 0,
                        var userId: Long = 0,
                        var provider: String = "",
                        var social_id: String = "",
                        var name: String = "",
                        var createdAt: Timestamp=new Timestamp(System.currentTimeMillis), 
                        var updatedAt: Timestamp=new Timestamp(System.currentTimeMillis)) 
  extends BaseDiddTableWithTimestamps {


}

object SocialFriend extends CompanionTable[SocialFriend] {

  def clear(userId: Long, provider: String) = {
    withTransaction {
      SocialFriends.deleteWhere(f => f.userId === userId and f.provider === provider)
    }
  }

  def create(userId: Long, provider: String, friends: Map[String, String]) = {
    friends.foreach { case (id: String, name: String) => {
      val f = SocialFriend(userId = userId, provider = provider, social_id = id, 
                           name = name)
      withTransactionFuture {
        SocialFriends.upsert(f)
      }
    }}
  }

}
