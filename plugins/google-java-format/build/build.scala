import cbt._

class Build(val context: Context) extends Plugin {
  override def dependencies =
    super.dependencies ++
    Resolver( mavenCentral ).bind(
      MavenDependency( "com.google.googlejavaformat", "google-java-format", "1.3" )
    )
}
