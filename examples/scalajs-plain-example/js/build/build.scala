import cbt._
class Build(val context: Context) extends ScalaJsBuild{
  override def name = "my-project"

  override def sources = super.sources ++ Seq(
    projectDirectory.getParentFile ++ "/shared"
  )

  override def dependencies = (
    super.dependencies ++
      Resolver( mavenCentral ).bind(
        "org.scala-js" %%% "scalajs-dom" % "0.9.0"
      )
  )

  override def scalaJsTargetFile =
    projectDirectory.getParentFile ++ ("/server/public/generated/" ++ name ++ ".js")
}
