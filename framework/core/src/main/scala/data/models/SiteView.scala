/*
 * Copyright (c) 2015. Didd, Inc All Rights Reserved
 */

package com.maiden.framework.data.models

import java.sql.Timestamp
import com.maiden.framework.common.helpers.{Hasher, TokenGenerator}
import com.maiden.framework.common.Types._
import DiddSchema._

case class SiteView(var id: Long=0, 
                var model: String="",
                var modelId: Long = 0,
                var viewedBy: Long = 0,
                var createdAt: Timestamp=new Timestamp(System.currentTimeMillis), 
                var updatedAt: Timestamp=new Timestamp(System.currentTimeMillis)) 
  extends BaseDiddTableWithTimestamps {


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
