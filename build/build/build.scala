package cbt_build.cbt.build
import cbt._
class Build(val context: Context) extends BuildBuild with CbtInternal{
  override def dependencies = (
    super.dependencies :+ cbtInternal.shared :+ plugins.scalariform
  )
}
