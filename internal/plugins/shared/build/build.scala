package cbt_build.cbt_internal.library_build_plugin
import cbt._
class Build(val context: Context) extends Plugin{
  override def dependencies = super.dependencies :+ plugins.sonatypeRelease
}
