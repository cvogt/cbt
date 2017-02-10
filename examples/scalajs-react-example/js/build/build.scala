import cbt._
class Build(val context: Context) extends ScalaJsBuild{
  override def projectName = "my-project"

  override def sources = super.sources ++ Seq(
    projectDirectory.getParentFile ++ "/shared"
  )

  override def dependencies = (
    super.dependencies ++
      Resolver( mavenCentral ).bind(
        //"org.scalatest" %%% "scalatest" % "3.0.0-RC2",
        "com.github.japgolly.scalajs-react" %%% "core" % "0.10.4", // for example
        // for example if you want explicitely state scala version
        "org.scala-js" % "scalajs-dom_sjs0.6_2.11" % "0.9.0"
      )
  )

  override protected def fastOptJSFile = {
    projectDirectory.getParentFile ++ "/server/public" ++ ("/"++super.fastOptJSFile.getName)
  }
}
