package cbt_build.cbt.test
import cbt._
class Build(val context: cbt.Context) extends BaseBuild{
  override def dependencies = super.dependencies :+ context.cbtDependency
  def apply = run
  override def run = {
    classes.flatMap( lib.discoverCbtMain ).head( context )
  }
  def args = context.args
}
