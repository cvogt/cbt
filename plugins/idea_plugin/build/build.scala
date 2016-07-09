import cbt._

class Build(val context: Context) extends Plugin {
  override def dependencies =
    super.dependencies ++
      Resolver(mavenCentral).bind(
        ScalaDependency("org.scala-lang.modules", "scala-xml_2.11", "1.0.5")
      )
}
