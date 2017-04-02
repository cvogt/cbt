package cbt_build.reflect
import cbt._
import cbt_internal._
class Build(val context: Context) extends Library{
  override def inceptionYear = 2017
  override def description = "helpers to work with java io and nio"
  override def dependencies = super.dependencies :+ libraries.cbt.common_1
}
