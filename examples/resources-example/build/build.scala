import cbt._
class Build(val context: Context) extends DynamicOverrides{
  override def dependencies =
    super.dependencies ++ // don't forget super.dependencies here for scala-library, etc.
    Seq(
      DirectoryDependency( projectDirectory ++ "/parent" )
    )
}
