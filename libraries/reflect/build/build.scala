package cbt_build.reflect
import cbt._
import cbt_internal._
class Build(val context: Context) extends Library{
  override def inceptionYear = 2017
  override def description = "discover classes on your classpath and invoke methods reflectively, preventing System.exit"
  override def dependencies = super.dependencies :+ libraries.file
}
