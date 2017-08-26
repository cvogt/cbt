import cbt._

class Build(val context: Context) extends Plugin with Ensime {
  def scalaXBVersion = "1.5.2"
  
  override def dependencies = super.dependencies ++ Resolver(mavenCentral).bind(
    ScalaDependency("org.scalaxb", "scalaxb", scalaXBVersion)
  )
}
