import cbt._
import java.nio.file.Files._

class Build(val context: Context) extends PackageJars{
  override def name = "build-info-example"
  def groupId = "cbt.examples"
  override def defaultScalaVersion = "2.11.8"
  def version = "0.1"
  override def compile = { buildInfo; super.compile }
  def buildInfo = lib.writeIfChanged(
    projectDirectory / "src_generated/BuildInfo.scala",
    s"""// generated file
import java.io._
object BuildInfo{
def artifactId   = "$artifactId"
def groupId      = "$groupId"
def version      = "$version"
def scalaVersion = "$scalaVersion"
}
"""
  )
}
