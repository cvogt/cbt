package cbt

import cbt._
import java.nio.file.Files._
import java.nio.file._
import java.io.File

trait Scalafix extends BaseBuild {
  def scalafixVersion = "0.3.1"

  override def scalacOptions = super.scalacOptions ++
   Scalafix.scalacOptions(projectDirectory.toPath,
    Resolver( mavenCentral, sonatypeReleases ).bindOne(
        ScalaDependency( "ch.epfl.scala", "scalafix-nsc", scalafixVersion )
    ).jar)
}

object Scalafix {
  def scalacOptions( rootPath: Path, nscJar: File ) =
    Seq(
      "-Xplugin:" ++ nscJar.string,
      "-Yrangepos"
    ) ++ configOption(rootPath)

  def configOption( rootPath: Path ) =
    Some( rootPath.resolve(".scalafix.conf").toAbsolutePath )
      .filter(isRegularFile(_)).map("-P:scalafix:" ++ _.toString).toSeq
}
