package cbt

trait PublishDynamic extends Publish with DynamicOverrides{
  def snapshot = newBuild[PublishDynamic]{"""
    override def version = super.version ++ "-SNAPSHOT"
  """}
}
