import cbt._
class Build(val context: Context) extends BaseBuild{
  override def dependencies = super.dependencies :+ libraries.cbt.process
}