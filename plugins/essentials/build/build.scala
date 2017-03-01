import cbt._
// TODO: maybe move this back into stage2 to avoid having to call zinc separately for this as a plugin
// and to avoid the special casing "BuildBuildWithoutEssentials"
class Build(val context: Context) extends BaseBuild{
  override def dependencies = (
    super.dependencies
    :+ context.cbtDependency
    :+ libraries.eval
  )
}
