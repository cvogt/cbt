package cbt
class plugins( implicit context: Context ) {
  // TODO: move this out of the OO
  private def plugin( dir: String ) = DirectoryDependency( context.cbtHome / "plugins" / dir )
  final lazy val googleJavaFormat = plugin( "google-java-format" )
  final lazy val proguard = plugin( "proguard" )
  final lazy val sbtLayout = plugin( "sbt_layout" )
  final lazy val scalafix = plugin( "scalafix" )
  final lazy val scalafmt = plugin( "scalafmt" )
  final lazy val scalaJs = plugin( "scalajs" )
  final lazy val scalariform = plugin( "scalariform" )
  final lazy val scalaTest = plugin( "scalatest" )
  final lazy val sonatypeRelease = plugin( "sonatype-release" )
  final lazy val uberJar = plugin( "uber-jar" )
  final lazy val wartremover = plugin( "wartremover" )
}
