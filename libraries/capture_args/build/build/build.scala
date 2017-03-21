package cbt_build.cbt.capture_args.build
import cbt._
class Build(val context: Context) extends BuildBuild with CbtInternal{
  override def dependencies = super.dependencies :+ cbtInternal.library
}
