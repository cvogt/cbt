package cbt
import java.io.File
trait GenerateBuildInfo extends BaseBuild{
  override def compile = { buildInfo.apply; super.compile }
  def buildInfo = GenerateBuildInfo.apply( lib ).config(
    s"""
  def scalaVersion = "$scalaVersion"
""",
    None,
    "BuildInfo",
    projectDirectory / "src_generated"
  )
}
object GenerateBuildInfo{
  case class apply( lib: Lib ){
    case class config( body: String, `package`: Option[String], className: String, file: File ){
      def apply = {
        lib.writeIfChanged(
          file / className ++ ".scala",
          s"""// generated file${`package`.map("\npackage "++_++"").getOrElse("")}
object $className{
  $body
}
"""
        )
      }
    }
  }
}
