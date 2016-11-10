import cbt._
class Build(val context: Context) extends Plugin{
  override def dependencies =
    super.dependencies :+ // don't forget super.dependencies here for scala-library, etc.
    DirectoryDependency( context.cbtHome ++ "/libraries/eval" )
}
