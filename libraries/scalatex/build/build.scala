package scalatex_build
import cbt._
class Build(val context: Context) extends BaseBuild{
  override def dependencies = (
    super.dependencies ++ // don't forget super.dependencies here for scala-library, etc.
    Seq(
      libraries.cbt.eval
    ) ++
    Resolver( mavenCentral, sonatypeReleases ).bind(
      ScalaDependency( "com.lihaoyi", "scalatex-api", "0.3.7" ),
      ScalaDependency( "com.lihaoyi", "scalatex-site", "0.3.7" ),
      ScalaDependency("org.cvogt","scala-extensions","0.5.0"),
      ScalaDependency("org.scalameta", "scalameta", "1.6.0")
    )
  )
}
