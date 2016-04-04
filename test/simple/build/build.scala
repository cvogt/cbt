import cbt._
import scala.collection.immutable.Seq
import java.io.File

class Build(context: cbt.Context) extends BasicBuild(context){
  override def dependencies = (
    super.dependencies
    ++
    Seq(
      GitDependency("https://github.com/xdotai/diff.git", "8b501902999fe76d49e04937c4bd6d0b9e07b4a6"),
      MavenRepository.central.resolve(
        ScalaDependency("com.typesafe.play", "play-json", "2.4.4"),
        MavenDependency("joda-time", "joda-time", "2.9.2"),
        // the below tests pom inheritance with dependencyManagement and variable substitution for pom properties
        MavenDependency("org.eclipse.jgit", "org.eclipse.jgit", "4.2.0.201601211800-r"),
        // the below tests pom inheritance with variable substitution for pom xml tag contents
        MavenDependency("com.spotify", "missinglink-core", "0.1.1")
      ),
      MavenRepository.combine(
        MavenRepository.central,
        MavenRepository.bintray("tpolecat"),
        MavenRepository.sonatypeSnapshots
      ).resolve(
        "org.cvogt" %% "play-json-extensions" % "0.8.0",
        "org.tpolecat" %% "tut-core" % "0.4.2",
        "ai.x" %% "lens" % "1.0.0-SNAPSHOT"
      )
    )
  )
}
