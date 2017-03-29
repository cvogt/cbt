import cbt._

class Build(val context: Context) extends GenerateBuildInfo{
  def scalaTestVersion = "2.2.6"
  override def dependencies = (
    super.dependencies
    :+ context.cbtDependency
    ScalaDependency("org.scalatest","scalatest",scalaTestVersion)
  )
  override def buildInfo = super.buildInfo.copy(
    s"""def scalaTestVersion = "$scalaTestVersion"""",
    Some("cbt_plugins.scalaTest")
  )
}
