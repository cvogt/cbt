package cbt_build.common_1
import cbt._
import cbt_internal._
class Build(val context: Context) extends Library{
  override def inceptionYear = 2017
  override def description = "classes shared by multiple cbt libraries and needed in stage 1"
  override def dependencies = super.dependencies :+ libraries.cbt.common_0 :+ libraries.cbt.interfaces
}
