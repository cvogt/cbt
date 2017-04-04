import cbt._

class Build(val context: Context) extends BaseBuild with Scalastyle{
  override def scalastyle = super.scalastyle.copy(
    Scalastyle.readConfigFromXml( projectDirectory / "scalastyle-config.xml" )
  )
}
