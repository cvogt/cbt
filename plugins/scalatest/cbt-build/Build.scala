import cbt._

class Build(val context: Context) extends Plugin{
  override def dependencies =
    super.dependencies ++
    Resolver( mavenCentral ).bind(
      ScalaDependency("org.scalatest","scalatest","2.2.4")
    )
}
