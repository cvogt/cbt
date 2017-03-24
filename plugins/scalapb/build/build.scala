import cbt._

class Build(val context: Context) extends Plugin {
  private val scalaPBVersion = "0.5.47"

  override def dependencies =
    super.dependencies ++
    Resolver( mavenCentral ).bind(
      ScalaDependency( "com.trueaccord.scalapb", "scalapbc", scalaPBVersion )
    )

  override def compile = { buildInfo; super.compile }

  def buildInfo = lib.writeIfChanged(
    projectDirectory / "src_generated/BuildInfo.scala",
    s"""// generated file
package cbt.scalapb
object BuildInfo{
  def scalaPBVersion = "$scalaPBVersion"
}
"""
  )
}
