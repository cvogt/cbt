import cbt._
class Build(val context: cbt.Context) extends BaseBuild{
  override def dependencies = super.dependencies :+ context.cbtDependency
  def apply = run
  def args = context.args
}
