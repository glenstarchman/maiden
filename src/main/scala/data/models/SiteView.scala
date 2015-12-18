/*
 * Copyright (c) 2015. Maiden, Inc All Rights Reserved
 */

package com.maiden.data.models

import java.sql.Timestamp
import com.maiden.common.helpers.{Hasher, TokenGenerator}
import com.maiden.common.Types._
import MaidenSchema._

case class SiteView(var id: Long=0, 
                var model: String="",
                var modelId: Long = 0,
                var viewedBy: Long = 0,
                var createdAt: Timestamp=new Timestamp(System.currentTimeMillis), 
                var updatedAt: Timestamp=new Timestamp(System.currentTimeMillis)) 
  extends BaseMaidenTableWithTimestamps {


}

object SiteView extends CompanionTable[SiteView] {

  def getCountForObject(model: String, modelId: Long) = fetchOne {
    from(SiteViews)(sv => 
    where(sv.model === model and sv.modelId === modelId)
    compute(count()))
  }.headOption match {
    case Some(x) => x.measures
    case _ => 0
  }


  def create(model: String, modelId: Long, viewedBy: Long = 0) = withTransactionFuture {
    val p = SiteView(model = model, modelId = modelId, viewedBy = viewedBy)
    SiteViews.upsert(p)
    p
  }
}
