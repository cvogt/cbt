import cbt._

class Build(val context: Context) extends BaseBuild {

  override def dependencies = {
    super.dependencies ++ Resolver(mavenCentral).bind(
      MavenDependency("org.eclipse.jetty", "jetty-server", "9.3.12.v20160915"),
      MavenDependency("org.scalaj", "scalaj-http_" + constants.scalaMajorVersion, "2.3.0")
    )
  }

}
