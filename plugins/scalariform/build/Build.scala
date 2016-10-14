import cbt._

class Build(val context: Context) extends Plugin {
  override def dependencies =
    super.dependencies ++
    Resolver( mavenCentral ).bind(
      ScalaDependency("org.scalariform", "scalariform", "0.1.8")
    )
}
