import cbt._
import scala.collection.immutable.Seq
import java.io.File

// cbt:https://github.com/cvogt/cbt.git#bdd6d905807a8cee7655d436401e76196ec4fe67
class Build(context: cbt.Context) extends BasicBuild(context){
  override def dependencies = (
    super.dependencies
    ++
    Seq(
      GitDependency("https://github.com/xdotai/diff.git", "698717469b8dd86e8570b86354892be9c0654caf"),
      MavenResolver(context.cbtHasChanged,context.paths.mavenCache,MavenResolver.central).resolve(
        ScalaDependency("com.typesafe.play", "play-json", "2.4.4"),
        MavenDependency("joda-time", "joda-time", "2.9.2"),
        // the below tests pom inheritance with dependencyManagement and variable substitution for pom properties
        MavenDependency("org.eclipse.jgit", "org.eclipse.jgit", "4.2.0.201601211800-r"),
        // the below tests pom inheritance with variable substitution for pom xml tag contents
        MavenDependency("com.spotify", "missinglink-core", "0.1.1")
      ),
      MavenResolver(
        context.cbtHasChanged,
        context.paths.mavenCache,
        MavenResolver.central,
        MavenResolver.bintray("tpolecat"),
        MavenResolver.sonatypeSnapshots
      ).resolve(
        "org.cvogt" %% "play-json-extensions" % "0.8.0",
        "org.tpolecat" %% "tut-core" % "0.4.2",
        "ai.x" %% "lens" % "1.0.0-SNAPSHOT"
      )
    )
  )
}
