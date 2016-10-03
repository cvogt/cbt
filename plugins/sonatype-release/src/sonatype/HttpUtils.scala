package cbt.sonatype

import java.net.URL

import cbt.Stage0Lib

import scala.annotation.tailrec
import scala.util.{ Failure, Success, Try }

private[sonatype] object HttpUtils {
  // Make http GET. On failure request will be retried with exponential backoff.
  def GET(uri: String, headers: Map[String, String]): (Int, String) =
  withRetry(httpRequest("GET", uri, headers))

  // Make http POST. On failure request will be retried with exponential backoff.
  def POST(uri: String, body: Array[Byte], headers: Map[String, String]): (Int, String) =
  withRetry(httpRequest("POST", uri, headers, body))

  private def httpRequest(method: String, uri: String, headers: Map[String, String], body: Array[Byte] = Array.emptyByteArray): (Int, String) = {
    val conn = Stage0Lib.openConnectionConsideringProxy(new URL(uri))
    conn.setReadTimeout(60000) // 1 minute
    conn.setConnectTimeout(30000) // 30 seconds

    headers foreach { case (k,v) =>
      conn.setRequestProperty(k, v)
    }
    conn.setRequestMethod(method)
    if(method == "POST" || method == "PUT") { // PATCH too?
      conn.setDoOutput(true)
      conn.getOutputStream.write(body)
    }

    val arr = new Array[Byte](conn.getContentLength)
    conn.getInputStream.read(arr)

    conn.getResponseCode -> new String(arr)
  }

  // ============== General utilities

  def withRetry[T](f: => T): T = withRetry(4000, 5)(f)

  /**
    * Retry execution of `f` `retriesLeft` times
    * with `delay` doubled between attempts.
    */
  @tailrec
  def withRetry[T](delay: Int, retriesLeft: Int)(f: ⇒ T): T = {
    Try(f) match {
      case Success(result) ⇒
        result
      case Failure(e) ⇒
        if (retriesLeft == 0) {
          throw new Exception(e)
        } else {
          val newDelay = delay * 2
          val newRetries = retriesLeft - 1
//          log(s"Failed with exception: $e, will retry $newRetries times; waiting: $delay")
          Thread.sleep(delay)

          withRetry(newDelay, newRetries)(f)
        }
    }
  }
}
