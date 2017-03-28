package cbt_examples.fork_example

import akka.http.scaladsl.server._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.settings._

object Service extends HttpApp with App {
  // should all appear in separate lines
  System.out.println("HelloHello")
  System.err.println("HelloHello")
  System.out.println("HelloHello")
  System.err.println("HelloHello")
  System.out.println("HelloHello")
  System.err.println("HelloHello")
  System.out.println("HelloHello")
  System.err.println("HelloHello")
  System.out.println("HelloHello")
  System.err.println("HelloHello")

  override protected def route =
    path("test") {
      get {
        complete(HttpResponse())
      }
    }

  startServer("localhost", 8080)
}
