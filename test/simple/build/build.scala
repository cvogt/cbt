import cbt._
import scala.collection.immutable.Seq
import java.io.File

class Build(context: cbt.Context) extends BasicBuild(context){
  override def dependencies = (
    super.dependencies
    ++
    Seq(
      GitDependency("https://github.com/xdotai/diff.git", "666bbbf4dbff6fadc81c011ade7b83e91d3f9256"),
      MavenRepository.central.resolve(
        ScalaDependency("com.typesafe.play", "play-json", "2.4.4"),
        MavenDependency("joda-time", "joda-time", "2.9.2"),
        // the below tests pom inheritance with dependencyManagement and variable substitution for pom properties
        MavenDependency("org.eclipse.jgit", "org.eclipse.jgit", "4.2.0.201601211800-r"),
        // the below tests pom inheritance with variable substitution for pom xml tag contents
        MavenDependency("com.spotify", "missinglink-core", "0.1.1"),
        // the below tests pom inheritance with variable substitution being parts of strings
        MavenDependency("cc.factorie","factorie_2.11","1.2")
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
