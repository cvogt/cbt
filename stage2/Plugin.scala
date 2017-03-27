package cbt
trait Plugin extends BaseBuild{
  override def dependencies = super.dependencies :+ context.cbtDependency
  object plugins extends plugins( context, scalaVersion )

  /** CBT relies on hierarchical classloaders */
  final override def flatClassLoader = false
}
