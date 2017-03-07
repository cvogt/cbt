package cbt

trait SnapshotVersion extends ArtifactInfo with DynamicOverrides{
  def snapshot = newBuild[SnapshotVersion]{"""
    override def version = super.version ++ "-SNAPSHOT"
  """}
}
