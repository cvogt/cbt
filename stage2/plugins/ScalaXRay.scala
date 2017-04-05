package cbt
import java.io.File
trait ScalaXRay extends BaseBuild{
  override def scalacOptions = {
    super.scalacOptions ++ ScalaXRay.scalacOptions(
      ports.scalaXRay.dependency.asInstanceOf[PackageJars]
    )
  }
}
object ScalaXRay{
  def scalacOptions( plugin: PackageJars, linkFile: Option[File] = None ) =
    Seq(
      "-Xplugin:" ~ plugin.jar.get.string,
      "-Xplugin-require:sxr"
      //"-P:sxr:base-directory:" ~ baseDirectory.string
    ) ++ linkFile.map( "-P:sxr:link-file:" ~ _.string )
}
