package com.maiden.common

//import java.util._
import org.json4s.jackson.Serialization
import org.json.JSONObject
import com.pubnub.api._
import com.maiden.common.MaidenConfigFactory.config

object PubnubHelper {
  implicit val formats = org.json4s.DefaultFormats

  val pubKey = config("pubnub.publish_key").toString 
  val subKey = config("pubnub.subscribe_key").toString
  val secretKey = config("pubnub.secret_key").toString

  val pubnub = new Pubnub(pubKey, subKey, secretKey)

  def createChannel(channelName: String) = {

  }

  def closeChannel(channelName: String) = {

  }

  def send(channelName: String, message: Map[String, Any]) = {
  
    var callback = new Callback() {
      override def successCallback(channel: String, message: Object) {
        //println("PUBLISH : " + message);
      }
      override def errorCallback(channel: String, error: PubnubError) {
        //println("PUBLISH ERROR: " + error);
      }
    }

    val msg = new JSONObject(Serialization.write(message))
    //val msg = new JSONObject(message.map { case(k,v) => k.toString -> v.toStringasJava)
    pubnub.publish(channelName, msg,  callback)

  }
}
