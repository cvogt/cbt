package cbt_build.cbt.build
import cbt._
class Build(val context: Context) extends CbtInternal{
  override def dependencies = (
    super.dependencies :+ cbtInternal.shared
  )
}
