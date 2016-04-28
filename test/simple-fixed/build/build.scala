import cbt._
import cbt.extensions._ // FIXME: do not require this import
import scala.collection.immutable.Seq
import java.io.File

// cbt:file:///Users/chris/code/cbt/#b65159f95421d9484f29327c11c0fa179eb7483f
class Build(context: cbt.Context) extends BasicBuild(context){
  override def dependencies = (
    super.dependencies
    ++
    Seq(
      GitDependency("https://github.com/xdotai/diff.git", "80a08bf45f7c4c3fd20c4bc6dbc9cae0072e3c0f"),
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
