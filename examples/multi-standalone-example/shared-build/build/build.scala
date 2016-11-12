import cbt._
class Build(val context: Context) extends BaseBuild{
  override def dependencies =
    super.dependencies :+ // don't forget super.dependencies here
      context.cbtDependency
}
