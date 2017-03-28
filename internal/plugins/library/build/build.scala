package cbt_build.cbt_internal.library_build_plugin
import cbt._
class Build(val context: Context) extends Plugin with CbtInternal{
  override def dependencies = (
    super.dependencies :+ cbtInternal.shared :+ plugins.scalariform :+ plugins.googleJavaFormat
  )
}
