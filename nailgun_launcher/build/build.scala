package cbt_build.nailgun_launcher
import cbt._
class Build(val context: Context) extends BaseBuild{
  override def dependencies = (
    super.dependencies ++ // don't forget super.dependencies here for scala-library, etc.
    Seq(
      DirectoryDependency( projectDirectory / ".." / "common-0" )
    )
  )
}
