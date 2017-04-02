package cbt_examples.scalatex_example_build
import cbt._
import java.io._
import java.nio.file._
import java.nio.file.attribute._
class Build(val context: Context) extends Scalatex{
  override def dependencies = super.dependencies ++ Seq(
    DirectoryDependency( projectDirectory / ".." )
  )
  override def run = {
    scalatex.apply
    super.run
  }
}
