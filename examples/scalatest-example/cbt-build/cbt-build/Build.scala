import cbt._

class Build(val context: Context) extends BuildBuild{
  override def dependencies = super.dependencies ++ Seq(
    plugins.scalaTest,
    plugins.sbtLayout
  )
}
