import cbt._
import scala.collection.immutable.Seq
import java.io.File

class Build(context: cbt.Context) extends BasicBuild(context){
  override def dependencies = (
    super.dependencies
    ++
    Seq(
      GitDependency("https://github.com/xdotai/diff.git", "698717469b8dd86e8570b86354892be9c0654caf"),
      // FIXME: make the below less verbose
      MavenResolver(context.cbtHasChanged,context.paths.mavenCache,MavenResolver.central).resolve(
        ScalaDependency("com.typesafe.play", "play-json", "2.4.4"),
        MavenDependency("joda-time", "joda-time", "2.9.2"),
        // the below tests pom inheritance with dependencyManagement and variable substitution for pom properties
        MavenDependency("org.eclipse.jgit", "org.eclipse.jgit", "4.2.0.201601211800-r"),
        // the below tests pom inheritance with variable substitution for pom xml tag contents
        MavenDependency("com.spotify", "missinglink-core", "0.1.1"),
        // the below tests pom inheritance with variable substitution being parts of strings
        MavenDependency("cc.factorie","factorie_2.11","1.2")
        // the dependency below uses a maven version range. Currently not supported.
        // TODO: put in a proper error message for version range not supported
        //MavenDependency("com.github.nikita-volkov", "sext", "0.2.4")
        // currently breaks with can't find https://repo1.maven.org/maven2/org/apache/avro/avro-mapred/1.7.7/avro-mapred-1.7.7-hadoop2.pom.sha1
        // org.apache.spark:spark-sql_2.11:1.6.1
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
