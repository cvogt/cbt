import cbt._

class Build(val context: Context) extends BuildBuildWithoutEssentials{
  // this is used by the essentials plugin, so we can't depend on it
  override def dependencies = super.dependencies ++ Seq(
    plugins.scalaTest
  )
}
