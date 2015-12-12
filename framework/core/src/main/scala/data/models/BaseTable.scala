/*
 * Copyright (c) 2015. Didd, Inc All Rights Reserved
 */

package com.maiden.framework.data.models

import scala.reflect.runtime.{ universe => ru }
import scala.concurrent.duration._
import java.sql.Timestamp
import org.squeryl._
import com.maiden.framework.common.converters._
import com.maiden.framework.common.Types._
import com.maiden.framework.data.models.DiddSchema._
import com.maiden.framework.common.DiddCache._
import com.maiden.framework.common.helpers.{Text}

trait BaseDiddTable extends Convertable with KeyedEntity[Long] 

trait NonKeyedBaseDiddTable extends Convertable

trait Adminable extends BaseDiddTable {

  private[this] def prettyType(s: String) = s.split('.').toList.last.toLowerCase 
  private[this] def prettyName(s: String) = Text.titleize(s)

  private[this] def getHtmlType(s: String) = {
    prettyType(s) match {
      case "string" => "string"
      case "date" | "datetime" | "timestamp" => "date"
      case "long" | "double" | "int" | "float" => "number"
      case x: String => x
    }
  }

  def getHtmlFieldMappings () = {
    val m = Mapper.ccToMap(this) 
    val fields = getClass.getDeclaredFields
    fields
      .filter(f => f.getName != "_isPersisted")
      .map(f => 
        f.getName -> Map(
          "prettyName" -> prettyName(f.getName), 
          "type" -> prettyType(f.getType.toString),
          "value" -> m(f.getName) 
        )
      ).toMap 
  }

  def buildHtmlEditFields() = {
    val fields = getHtmlFieldMappings
    fields.map { case (name, o) => { 
      val value = xml.Utility.escape(o.getOrElse("value", "").toString)
      o("type") match {
        case _ => s"<input type='text' id='${name}' value='${value}'"

      }
    }}.toList
  }

  def buildHtmlViewFields() = {
    val fields = getHtmlFieldMappings
    fields.map { case (name, o) => { 
      val value = xml.Utility.escape(o.getOrElse("value", "").toString)
      o("type") match {
        case _ => s"<span class='data-view' id='${name}'>${value}</span>"

      }
    }}.toList
  }
}

trait BaseDiddTableWithTimestamps extends BaseDiddTable with Adminable {
  var id: Long
  var createdAt: Timestamp 
  var updatedAt: Timestamp 

  def modelName = getClass.getName.split('.').last
  def baseKey = s"${modelName}:${id}"

}

trait FriendlyIdable extends BaseDiddTableWithTimestamps {

  def getNameField() = {
    val possibleFields = List("name", "userName")
    val values = productIterator
    val field = getClass
                .getDeclaredFields
                .filter(f => possibleFields.contains(f.getName))
                .toList
                .head
    field.setAccessible(true)
    field.get(this).toString
  }

  def friendlyId() = {
    val modelName = getClass.getName.split('.').last
    FriendlyId.generate(modelName, id, getNameField) match {
      case Some(fid) => fid.hash
      case _ => id.toString
    }
  }
}

/* a Trait for all models that have a parent project.... 
   this automagically updates the project's `updatedAt` 
   on save
*/

trait NonKeyedBaseDiddTableWithTimestamps extends NonKeyedBaseDiddTable {
  var createdAt: Timestamp
  var updatedAt: Timestamp
}

/* singletons for models. extends the base model */
trait CompanionTable[T <: BaseDiddTableWithTimestamps] { 

  lazy val modelName = this.getClass.getName.split('.').last.replace("$", "")
  lazy val model = lookup(modelName)

  private def cacheKey(id: Long) = s"${modelName}:${id.toString}"

  /* simple helpers based on primary key */
  def get(id: Long): Option[T] = { 
    fetchOne {
      from(model)(m => 
      where(m.id === id)
      select(m))
    }.asInstanceOf[Option[T]]
  }

  def delete(id: Long) = {
    try {
      withTransaction {
        model.delete(id)
      }
      true
    } catch {
      case e: Exception => {
        println(e)
        false
      }
    }
  }

  def exists(id: Long) = {
    get(id) match {
      case Some(x) => true
      case _ => false
    }
  }
}

trait LoggableCompanionTable[T <: BaseDiddTableWithTimestamps] extends CompanionTable[T] 
