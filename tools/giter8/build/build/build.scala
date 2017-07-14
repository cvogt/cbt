import cbt._

class Build(val context: Context) extends BuildBuild {
  override def dependencies: Seq[Dependency] =
    super.dependencies ++
      Resolver(mavenCentral).bind(
        ScalaDependency("org.foundweekends.giter8", "giter8-lib", "0.9.0")
      )
}
