package cbt
class libraries( context: Context, scalaVersion: String, scalaMajorVersion: String ) {
  private def dep( name: String ) = DirectoryDependency(
    context.copy(
      scalaVersion     = Some( scalaVersion ),
      workingDirectory = context.cbtHome / "libraries" / name
    ),
    None
  )
  object cbt {
    def captureArgs = dep( "capture_args" )
    def common_0 = dep( "common-0" )
    def common_1 = dep( "common-1" )
    def eval = dep( "eval" )
    def file = dep( "file" )
    def interfaces = dep( "interfaces" )
    def proguard = dep( "proguard" )
    def reflect = dep( "reflect" )
    def scalatestRunner = dep( "scalatest-runner" )
  }
  object scala {
    private def _maven = ( g: String, a: String, v: String ) => {
      MavenResolver(
        context.cbtLastModified, context.paths.mavenCache, mavenCentral
      )(
          context.logger, context.transientCache, context.classLoaderCache
        ) bindOne MavenDependency( g, a, v )
    }

    def library = _maven( "org.scala-lang", "scala-library", scalaVersion )
    def reflect = _maven( "org.scala-lang", "scala-reflect", scalaVersion )
    def compiler = _maven( "org.scala-lang", "scala-compiler", scalaVersion )
    def xml = _maven( "org.scala-lang.modules", "scala-xml_" ++ scalaMajorVersion, "1.0.6" )
    def parserCombinators = _maven( "org.scala-lang.modules", "scala-parser-combinators_" ++ scalaMajorVersion, "1.0.5" )
  }
}
