/* an HTTP client helper based on http4s */
package com.maiden.common

import java.util.concurrent.ScheduledExecutorService
import java.io._
import java.util.concurrent.TimeoutException
import scala.concurrent.duration._
import org.http4s._
import org.http4s.Method._
import org.http4s.client._
import org.http4s.client.blaze._
import scodec.bits.ByteVector
import scalaz.concurrent.Task
import org.http4s.Status.NotFound
import org.http4s.Status.ResponseClass.Successful
import org.json4s._
import org.json4s.native.JsonMethods._
import com.maiden.common.exceptions._
import com.maiden.common.helpers.FileWriter

class HttpClient(url: String, timeout: Duration = 30 second, method: String = "GET", data: Map[String, Seq[String]] = Map.empty ) {

  implicit val formats = DefaultFormats
  val baseClient = middleware.FollowRedirect(1)(defaultClient)
  val req = method match {
    case "POST" => POST(getUri(url), UrlForm(data))
    case "PUT" => PUT(getUri(url), UrlForm(data))
    case "DELETE" => DELETE(getUri(url))
    case _ => GET(getUri(url))
  }

  val client = baseClient(req)

  def getUri(s: String): Uri = 
    Uri.fromString(s).getOrElse(throw(new InvalidUrlException(message=s)))

  private[this] def asJson(s: String) = try {
    parse(s)
  } catch {
    case e: Exception => throw(new ExternalResponseException(
                            message = url, exc = Option(e)))
  }

  private[this] def asMap(s: String) = try {
    asJson(s).extract[Map[String, Any]]
  } catch {
    case e: Exception => throw(new ExternalResponseException(
                            message = url, exc = Option(e)))
  }

  def fetchRaw() = {
    val res = client.flatMap {
      case Successful(resp) => resp.as[ByteVector].map(x=>x)
      case NotFound(resp) => throw(new UrlNotFoundException(message=url))
      case resp => throw(new ExternalResponseException(message = resp.toString)) 
    }

    try {
      res.timed(timeout).run
    } catch {
      case e: TimeoutException  => throw(new ExternalResponseTimeoutException(message = url))
      case e: Exception => {
        throw(new ExternalResponseException(message = url, exc=Option(e)))
      }
    }
  }

  private[this] def fetchAsString() = fetchRaw
    .asInstanceOf[ByteVector]
    .toIterable
    .map(_.toChar)
    .mkString("") 

  def fetch() = fetchAsString
  def fetch[T](callback : (String) => T) = callback(fetchAsString) 
  def fetchAsMap() = fetch(asMap)
  def fetchAsJson() = fetch(asJson) 

  /* fetch a resource from a URL and save as a file */
  def fetchAsFile(fileName: String) = {
    val r = fetchRaw.asInstanceOf[ByteVector].toIterable.toArray
		FileWriter.write(r, fileName)
		new File(fileName)
  }
}
