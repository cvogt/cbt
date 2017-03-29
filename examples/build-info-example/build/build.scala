import cbt._
import java.nio.file.Files._

class Build(val context: Context) extends PackageJars with GenerateBuildInfo{
  override def name = "build-info-example"
  def groupId = "cbt.examples"
  override def defaultScalaVersion = "2.11.8"
  def version = "0.1"
  override def buildInfo = super.buildInfo.copy(
    s"""
  def artifactId   = "$artifactId"
  def groupId      = "$groupId"
  def version      = "$version"
  def scalaVersion = "$scalaVersion"
"""
  )
}
