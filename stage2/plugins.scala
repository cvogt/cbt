package cbt
class plugins( context: Context, scalaVersion: String ) {
  private def plugin( name: String ) = DirectoryDependency(
    context.copy(
      scalaVersion     = Some( scalaVersion ),
      workingDirectory = context.cbtHome / "plugins" / name
    ),
    None
  )
  final lazy val googleJavaFormat = plugin( "google-java-format" )
  final lazy val proguard = plugin( "proguard" )
  final lazy val sbtLayout = plugin( "sbt_layout" )
  final lazy val scalafix = plugin( "scalafix" )
  final lazy val scalafixCompilerPlugin = plugin( "scalafix-compiler-plugin" )
  final lazy val scalafmt = plugin( "scalafmt" )
  final lazy val scalaJs = plugin( "scalajs" )
  final lazy val scalapb = plugin( "scalapb" )
  final lazy val scalariform = plugin( "scalariform" )
  final lazy val scalaTest = plugin( "scalatest" )
  final lazy val sonatypeRelease = plugin( "sonatype-release" )
  final lazy val uberJar = plugin( "uber-jar" )
  final lazy val wartremover = plugin( "wartremover" )
}
