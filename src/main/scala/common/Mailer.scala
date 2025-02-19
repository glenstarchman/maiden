/*
 * Copyright (c) 2015. Maiden, Inc All Rights Reserved
 */

package com.maiden.common

import java.io.File
import scala.io.{Source, Codec}
import scala.concurrent.Await
import scala.util.{Failure, Success, Properties}
import scala.concurrent.duration._
import com.joypeg.scamandrill.client.{MandrillAsyncClient, MandrillBlockingClient}
import com.joypeg.scamandrill.utils._
import com.joypeg.scamandrill.models._
import com.maiden.common.MaidenConfigFactory.config
import com.maiden.data.models._
import com.maiden.data.models.MaidenSchema._
import com.maiden.common.helpers.{FileReader, AmazonS3}
import com.maiden.common.helpers.Text.slugify
import com.maiden.common.exceptions._

class Mailer {
  val cdn = config("aws.s3.base_url").toString
  val bucket = config("aws.s3.asset_bucket").toString
  lazy val env = Properties.envOrElse("MAIDEN_MODE", "dev-local")
  val client = MandrillAsyncClient
  val blockingClient = MandrillBlockingClient

  val defaultSenderEmail = config("mail.default.sender_email").toString
  val defaultSenderName = config("mail.default.sender_name").toString

  val testMessage = new MSendMsg(
    html = "<h1>test</h1>",
    text = "test",
    subject = "subject test",
    from_email = defaultSenderEmail,
    from_name = defaultSenderName,
    bcc_address = "glen@maiden.com",
    to = List(MTo("glen@maiden.com")),
    tracking_domain = "maiden.com",
    signing_domain = "maiden.com",
    return_path_domain = "maiden.com"
  )

  def test = client.messagesSend(MSendMessage(message=testMessage))

  private def buildRender(template: String, data: Map[String, Any]) = {
    val t = MTemplateRender(
      template_name = slugify(s"${env}_${template}"),
      template_content = List.empty,
      merge_vars = data.map{ case (k,v) =>
        MTemplateCnt(name = k.toUpperCase, content = v.toString)
      }.toList
    ) 
    Await.result(client.templateRender(t), 10 second)
  }

  def uploadAndPublishTemplate(templatePath: String, templateName: String) = {
    val templateBaseDir = s"${System.getProperty("user.dir")}/templates/email"

    val fullPath = s"${templateBaseDir}/${templatePath}.html"
    val basePath = fullPath.split('/').dropRight(1).mkString("/")
    val templateCode = Source.fromFile(fullPath)(Codec.UTF8).mkString

    val templateInfo = MTemplateInfo(
      name = templateName
    )

    val template = MTemplate(
      name = templateName,
      from_email = defaultSenderEmail,
      from_name = defaultSenderName,
      subject = "subject",
      code = templateCode,
      text = "text",
      publish = true,
      labels = List("email") 
    )
    //check if this template already exists
    val exists = blockingClient.templateInfo(templateInfo) match {
      case Success(res) => true 
      case Failure(ex) => false 
    }

    exists match {
      case true => 
        blockingClient.templateUpdate(template)
      case false =>
        blockingClient.templateAdd(template)
    }
    //grab the images in this template dir and upload to S3
    val images = FileReader.filesAtWithExtension(basePath, "png") ++
                 FileReader.filesAtWithExtension(basePath, "gif") ++
                 FileReader.filesAtWithExtension(basePath, "jpg")

    images.foreach(image => { 
      val s3Path = image.getPath.split("/").dropWhile(x => x != "email")
      AmazonS3.put(image, "images/" + s3Path.mkString("/"), bucket)
    })

    client.shutdown
  }

  private def buildMessage(to: String, title: String, 
                           d: MTemplateRenderResponse,
                           fromEmail: String, fromName: String) = {

    new MSendMsg(
      subject = title,
      from_email = fromEmail,
      from_name = fromName,
      to = List(MTo(to)),
      html = d.html.getOrElse(""),
      text = "",
      bcc_address = "",
      tracking_domain = "maiden.com",
      signing_domain = "maiden.com",
      return_path_domain = "maiden.com"
    )
  }

  def sendMail(to: String, title: String, template: String, 
               data: Map[String, Any] = Map.empty, 
               fromEmail: String = defaultSenderEmail, 
               fromName: String = defaultSenderName) = {

    val render = buildRender(template, data)
    val msg = MSendMessage(
      message = buildMessage(to, title, render, fromEmail, fromName),
      async = true
    )
    client.messagesSend(msg)
  }
}

object Mailer {
  def uploadTemplate(templatePath: String, templateName: String) {
    val mailer = new Mailer()
    mailer.uploadAndPublishTemplate(templatePath, templateName)
    println("done")
  }
}
