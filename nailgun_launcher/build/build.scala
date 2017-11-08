package cbt_build.nailgun_launcher
import cbt._
class Build(val context: Context) extends BaseBuild{
  // nailgun launcher doesn't need Scala. In fact not
  // removing it here will lead to conflicts when
  // is is loaded as a DirectoryDependency in
  // loadCustomBuildWithDifferentCbtVersion
  // because depending on a specific scala version conflicts
  // with whatever scala version the host cbt runs
  override def dependencies = Seq()
}
