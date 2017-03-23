package cbt_examples.akka_example

import akka.http.scaladsl.server._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.settings._

object Service extends HttpApp with App {
  override protected def route =
    path("test") {
      get {
        complete(HttpResponse())
      }
    }

  // CBT isolates it's classloaders. Even when using `direct` mode and a flatClassLoader,
  // it will not use the jvm's system classloader stack for the application.
  // This means if applications are hard-coded against the system classloader,
  // e.g. for loading configuration files from their users or their own jars,
  // they will not find them. One way around this is telling CBT to fork the process.
  // (via `override def fork = true` in your build)
  // This "just" works, but you'll loose all caching speedup benefits.
  // Alternatively, applications like akka often allow providing a custom
  // classloader instead and then benefit from CBT's classloader caching. Here is how:
  val system = akka.actor.ActorSystem( "my-actor-system", classLoader = Some(this.getClass.getClassLoader) )
  startServer("localhost", 8080, ServerSettings(system), system)
}
