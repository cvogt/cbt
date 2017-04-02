package cbt
trait Plugin extends BaseBuild{
  override def dependencies = super.dependencies :+ context.cbtDependency
  object plugins extends plugins( context, scalaVersion )
}
