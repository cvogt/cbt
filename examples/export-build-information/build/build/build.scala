import cbt._

class Build(val context: Context) extends BuildBuild {
  override def dependencies = 
    super.dependencies ++
    Resolver(mavenCentral, sonatypeReleases).bind(
      ScalaDependency( "org.scala-lang.modules", "scala-xml",  "1.0.6" )
    ) 
}
