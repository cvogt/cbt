package cbt

trait PublishDynamic extends Publish with DynamicOverrides{
  def publishSnapshotLocal: Unit =
    newBuild[PublishDynamic]{"""
      override def version = super.version ++ "-SNAPSHOT"
    """}.publishLocal
}
