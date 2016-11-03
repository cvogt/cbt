import cbt._
class Build(val context: Context) extends BaseBuild{
  /*
  override def dependencies =
    super.dependencies ++ // don't forget super.dependencies here
    Seq(    
      // source dependency
      DirectoryDependency( projectDirectory ++ "/subProject" )
    ) ++
    Resolver( mavenCentral ).bind(
      // CBT-style Scala dependencies
      ScalaDependency( "com.lihaoyi", "ammonite-ops", "0.5.5" )
      MavenDependency( "com.lihaoyi", "ammonite-ops_2.11", "0.5.5" )

      // SBT-style dependencies
      "com.lihaoyi" %% "ammonite-ops" % "0.5.5"
      "com.lihaoyi" % "ammonite-ops_2.11" % "0.5.5"
    )
  */
  override def resourceClasspath = super.resourceClasspath ++ ClassPath(Seq(projectDirectory ++ "/my-resources"))
}
