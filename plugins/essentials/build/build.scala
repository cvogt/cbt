import cbt._
class Build(val context: Context) extends BaseBuild{
  override def dependencies = (
    super.dependencies
    :+ context.cbtDependency
    :+ DirectoryDependency( context.cbtHome ++ "/libraries/eval" )
  )
}
