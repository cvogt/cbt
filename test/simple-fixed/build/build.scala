import cbt._

class Build(context: cbt.Context) extends BasicBuild(context){
  override def dependencies = (
    super.dependencies
    ++
    Seq(
      GitDependency("https://github.com/cvogt/cbt.git", "e8673866b79f7473391dcee26243eee80d5d3cb6", Some("test/library-test"))
    )
    ++
    Resolver(mavenCentral).bind(
      ScalaDependency("com.typesafe.play", "play-json", "2.4.4"),
      MavenDependency("joda-time", "joda-time", "2.9.2"),
      // the below tests pom inheritance with dependencyManagement and variable substitution for pom properties
      MavenDependency("org.eclipse.jgit", "org.eclipse.jgit", "4.2.0.201601211800-r"),
      // the below tests pom inheritance with variable substitution for pom xml tag contents
      MavenDependency("com.spotify", "missinglink-core", "0.1.1")
    )
    ++
    Resolver(
      mavenCentral,
      bintray("tpolecat"),
      sonatypeSnapshots
    ).bind(
      "org.cvogt" %% "play-json-extensions" % "0.8.0",
      "ai.x" %% "lens" % "1.0.0"
    )
  )
}
