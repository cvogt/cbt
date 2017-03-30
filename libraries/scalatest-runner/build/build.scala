import cbt._

class Build(val context: Context) extends BaseBuild{
  override def dependencies = super.dependencies ++
    Resolver( mavenCentral ).bind(
      ScalaDependency("org.scalatest","scalatest", if(scalaMajorVersion == "2.12") "3.0.1" else "2.2.6")
    )
}
