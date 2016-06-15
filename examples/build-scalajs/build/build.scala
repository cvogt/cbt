import cbt._
import java.net.URL
import java.io.File
import scala.collection.immutable.Seq

class Build( context: Context ) extends BasicBuild( context ) with ScalaJsBuild {

  override val projectName = "my-project"

  override def dependencies = (
    super.dependencies ++
      Resolver( mavenCentral ).bind(
        //"org.scalatest" %%% "scalatest" % "3.0.0-RC2",
        "com.github.japgolly.scalajs-react" %%% "core" % "0.10.4", // for example
        // for example if you want explicitely state scala version
        "org.scala-js" % "scalajs-dom_sjs0.6_2.11" % "0.9.0"
      )
  )

  /* ++ some JVM only dependencies */
  override def jvmDependencies = Seq.empty

  override def fastOptOutput = {
    projectDirectory.getAbsolutePath + "/server/public/" + new File(super.fastOptOutput).getName
  }
}

