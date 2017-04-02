package cbt
import java.io.File
trait ScalaTest extends BaseBuild {
  override def dependencies = super.dependencies :+ libraries.cbt.scalatestRunner
  override def run: ExitCode = {
    classLoader.loadClass( "cbt.scalatest.Runner" ).method(
      "run", classOf[Array[File]], classOf[ClassLoader]
    ).invoke( null, exportedClasspath.files.toArray, classLoader )
    ExitCode.Success
  }
}
