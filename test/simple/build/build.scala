import cbt._

class Build(val context: cbt.Context) extends BaseBuild{
  override def dependencies = (
    super.dependencies
    ++
    // FIXME: make the below less verbose
    Resolver( mavenCentral ).bind(
      ScalaDependency("com.typesafe.play", "play-json", "2.4.4"),
      ScalaDependency("io.spray","spray-http","1.3.3"),
      ScalaDependency( "com.lihaoyi", "scalatex-api", "0.3.6" ),
      ScalaDependency( "com.lihaoyi", "scalatex-site", "0.3.6" ),
      MavenDependency("joda-time", "joda-time", "2.9.2"),
      // the below tests pom inheritance with dependencyManagement and variable substitution for pom properties
      MavenDependency("org.eclipse.jgit", "org.eclipse.jgit", "4.2.0.201601211800-r"),
      // the below tests pom inheritance with variable substitution for pom xml tag contents
      MavenDependency("com.spotify", "missinglink-core", "0.1.1"),
      // the below tests pom inheritance with variable substitution being parts of strings
      MavenDependency("cc.factorie","factorie_2.11","1.2"),
      // test recursive substitution. see https://github.com/cvogt/cbt/issues/434
      MavenDependency("com.amazonaws", "aws-java-sdk-s3", "1.11.86"),
      // requires maven version ranges
      MavenDependency("io.grpc", "grpc-netty", "1.2.0")
      // the dependency below uses a maven version range with automatic minor version. Currently not supported.
      // TODO: put in a proper error message for version range not supported
      // MavenDependency("com.github.nikita-volkov", "sext", "0.2.4")
      // currently breaks with can't find https://repo1.maven.org/maven2/org/apache/avro/avro-mapred/1.7.7/avro-mapred-1.7.7-hadoop2.pom.sha1
      // ScalaDependency("org.apache.spark","spark-sql_2.11","1.6.1")
    ) ++
    Resolver( mavenCentral, sonatypeReleases ).bind(
      "org.scalameta" %% "scalameta" % "1.1.0"
    ).map(
      _.copy(
        // without this .replace the ScalatexCrash will crash during macro expansion
        replace = _ => _.map{
          case MavenDependency("com.lihaoyi","scalaparse_2.11",_,_,_) => "com.lihaoyi" % "scalaparse_2.11" % "0.3.1"
          case other => other
        }
      )
    ) ++
    Resolver( new java.net.URL("http://maven.spikemark.net/roundeights") ).bind(
      // Check that lower case checksums work
      ScalaDependency("com.roundeights","hasher","1.2.0")
    ) ++
    Resolver(
      mavenCentral,
      bintray("tpolecat"),
      sonatypeSnapshots
    ).bind(
      "org.cvogt" %% "play-json-extensions" % "0.8.0",
      "ai.x" %% "lens" % "1.0.0"
    )
  )

  def printArgs = context.args.mkString(" ")

  override def compile = {
    val dummyScalatexFile = projectDirectory / "src_generated" / "ScalatexCrash.scalatex"
    lib.write( dummyScalatexFile, "" )
    lib.write(
      projectDirectory / "src_generated" / "ScalatexCrash.scala",
      s"""object ScalatexCrash{
        import _root_.scalatags.Text.all._
        val file = _root_.scalatex.twf("${dummyScalatexFile}")
      }"""
    )
    super.compile
  }
}
