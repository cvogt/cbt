package cbt
import java.nio.file._
import java.io.File

class ConcreteBuildBuild(val context: Context) extends BuildBuild
class plugins(implicit context: Context){
  // TODO: move this out of the OO
  private def plugin(dir: String) = cbt.DirectoryDependency(context.cbtHome / "plugins" / dir)
  final lazy val googleJavaFormat = plugin( "google-java-format" )
  final lazy val proguard = plugin( "proguard" )
  final lazy val sbtLayout = plugin( "sbt_layout" )
  final lazy val scalafmt = plugin( "scalafmt" )
  final lazy val scalaJs   = plugin( "scalajs" )
  final lazy val scalariform = plugin( "scalariform" )
  final lazy val scalaTest = plugin( "scalatest" )
  final lazy val sonatypeRelease = plugin( "sonatype-release" )
  final lazy val uberJar = plugin( "uber-jar" )
  final lazy val wartremover = plugin( "wartremover" )
  final lazy val scalafix = plugin( "scalafix" )
}

trait BuildBuild extends BaseBuild{
  override def dependencies = super.dependencies :+ context.cbtDependency

  object plugins extends plugins

  assert(
    projectDirectory.getName === lib.buildDirectoryName,
    s"You can't extend ${lib.buildBuildClassName} in: " + projectDirectory + "/" + lib.buildDirectoryName
  )
}

trait CbtInternal extends BuildBuild{
  protected object cbtInternal{
    def shared = DirectoryDependency(context.cbtHome / "/internal/plugins/shared")
    def library = DirectoryDependency(context.cbtHome / "/internal/plugins/library")
  }
}
