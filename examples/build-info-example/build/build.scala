import cbt._
import java.nio.file.Files._

class Build(val context: Context) extends PackageJars{
  def name = "build-info-example"
  def groupId = "cbt.examples"
  def defaultVersion = "0.1"
  override def defaultScalaVersion = "2.11.8"
  override def compile = {
    val file = (projectDirectory ++ "/BuildInfo.scala").toPath
    val contents = s"""// generated file
import java.io._
object BuildInfo{
def artifactId   = "$artifactId"
def groupId      = "$groupId"
def version      = "$version"
def scalaVersion = "$scalaVersion"
}
"""
    if( exists(file) && contents != new String(readAllBytes(file)) )
      write(
        (projectDirectory ++ "/BuildInfo.scala").toPath,
        contents.getBytes
      )
    super.compile
  }
}
