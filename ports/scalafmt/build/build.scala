package migration_manager_build
import cbt._
class Build(val context: Context) extends BaseBuild{
  override def dependencies = Seq( phase2 )

  def git = GitDependency.checkout(
    "https://github.com/scalameta/scalafmt.git", "411ff5f370ad5a9fffa3e0e6407a0520a4825d88"
  )

  def phase2 = new Shared(context){
    override def dependencies: Seq[Dependency] = Seq(phase1)
    override def projectDirectory = {
      val f = super.projectDirectory / "phase2"
      f.mkdirs
      f
    }
    override def generatedSources = super.generatedSources ++ Seq(
      git / "core/src/main/scala",
      git / "cli/src/main/scala"
    )
  }

  def phase1 = new Shared(context){
    override def projectDirectory = {
      val f = super.projectDirectory / "phase1"
      f.mkdirs
      f
    }
    override def compile = { buildinfo; super.compile }
    def buildinfo = lib.writeIfChanged(
      projectDirectory / "src_generated/Versions.scala",
      s"""// generated file
package org.scalafmt
object Versions{
  def name = ???
  def version = ???
  def nightly = ???
  def stable = ???
  def scala = ???
  def coursier = ???
  def scalaVersion = "$scalaVersion"
  def sbtVersion = ???
}
"""
    )

    override def dependencies: Seq[Dependency] =
      Resolver( mavenCentral ).bind(
        "com.lihaoyi"    %% "sourcecode"      % "0.1.2",
        "org.scalameta"  %% "scalameta"       % "1.6.0",
        "org.scala-lang" % "scala-reflect"    % scalaVersion,
        "org.scala-lang" % "scala-compiler"    % scalaVersion,
        "com.typesafe"   % "config"           % "1.2.1",
        "com.geirsson"   %% "metaconfig-core" % "0.1.2",
        "com.martiansoftware" % "nailgun-server" % "0.9.1",
        "com.github.scopt"    %% "scopt"         % "3.5.0"
      )
    override def generatedSources = super.generatedSources ++ Seq(
      git / "utils/src/main/scala"
    )
  }
}

class Shared(val context: Context) extends BaseBuild{
  val meta = Resolver( mavenCentral ).bindOne(
    "org.scalameta" % "paradise_2.11.8" % "3.0.0-M7"
  )

  override def scalacOptions = (
    super.scalacOptions
    :+ ("-Xplugin:" ++ meta.jar.string)
    :+ "-Xplugin-require:macroparadise"
    :+ "-language:existentials"
  )
}
