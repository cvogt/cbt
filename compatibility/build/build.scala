package cbt_build.compatibility
import cbt._
class Build(val context: Context) extends BaseBuild{
  // don't depend on Scala, not needed
  override def dependencies = Seq()
}
