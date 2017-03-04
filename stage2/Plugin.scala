package cbt
trait Plugin extends BaseBuild{
  object plugins extends plugins

  override def dependencies =
    super.dependencies :+ context.cbtDependency :+ plugins.essentials
}
