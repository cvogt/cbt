package cbt_build.cbt.test
import cbt._
class Build(val context: cbt.Context) extends BaseBuild{
  override def dependencies = super.dependencies :+ context.cbtDependency
  def apply = run
  override def run = {
    classes.flatMap( lib.findCbtMain ).head( context )
  }
  def args = context.args
}
