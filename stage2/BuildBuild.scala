package cbt
import java.nio.file._
import java.io.File

class ConcreteBuildBuild(val context: Context) extends BuildBuild
class ConcreteBuildBuildWithoutEssentials(val context: Context) extends BuildBuildWithoutEssentials
trait BuildBuild extends BuildBuildWithoutEssentials{
  override def dependencies =
    super.dependencies :+ plugins.essentials
}
trait BuildBuildWithoutEssentials extends BaseBuild{
  assert(
    projectDirectory.getName === lib.buildDirectoryName,
    "You can't extend ${lib.buildBuildClassName} in: " + projectDirectory + "/" + lib.buildDirectoryName
  )

  protected final val managedContext = context.copy(
    workingDirectory = managedBuildDirectory,
    parentBuild=Some(this)
  )

  object plugins{
    // TODO: move this out of the OO
    final lazy val scalaTest = DirectoryDependency( context.cbtHome ++ "/plugins/scalatest" )
    final lazy val sbtLayout = DirectoryDependency( context.cbtHome ++ "/plugins/sbt_layout" )
    final lazy val scalaJs   = DirectoryDependency( context.cbtHome ++ "/plugins/scalajs" )
    final lazy val scalariform = DirectoryDependency( context.cbtHome ++ "/plugins/scalariform" )
    final lazy val scalafmt = DirectoryDependency( context.cbtHome ++ "/plugins/scalafmt" )
    final lazy val wartremover = DirectoryDependency( context.cbtHome ++ "/plugins/wartremover" )
    final lazy val uberJar = DirectoryDependency( context.cbtHome ++ "/plugins/uber-jar" )
    final lazy val sonatypeRelease = DirectoryDependency( context.cbtHome ++ "/plugins/sonatype-release" )
    final lazy val essentials = DirectoryDependency( context.cbtHome ++ "/plugins/essentials" )
  }

  override def dependencies =
    super.dependencies :+ context.cbtDependency
  def managedBuildDirectory: java.io.File = lib.realpath( projectDirectory.parent )
  def managedBuild = taskCache[BuildBuildWithoutEssentials]("managedBuild").memoize{
    val managedBuildFile = projectDirectory++("/"++lib.buildFileName)
    logger.composition("Loading build at " ++ managedBuildDirectory.toString)
    val build = (
      if( !managedBuildFile.exists ){
        throw new Exception(
          s"No file ${lib.buildFileName} (lower case) found in " ++ projectDirectory.getPath
        )
      } else {
        val contents = new String(Files.readAllBytes(managedBuildFile.toPath))
        val cbtUrl = ("cbt:"++GitDependency.GitUrl.regex++"#[a-z0-9A-Z]+").r
        cbtUrl
          .findFirstIn(contents)
          .flatMap{
            url =>
            val Array(base,hash) = url.drop(4).split("#")
            if(context.cbtHome.string.contains(hash))
              None
            else Some{
              // Note: cbt can't use an old version of itself for building,
              // otherwise we'd have to recursively build all versions since
              // the beginning. Instead CBT always needs to build the pure Java
              // Launcher in the checkout with itself and then run it via reflection.
              val dep = new GitDependency(base, hash, Some("nailgun_launcher"))
              val ctx = managedContext.copy( cbtHome = dep.checkout )
              dep.classLoader
                .loadClass( "cbt.NailgunLauncher" )
                .getMethod( "getBuild", classOf[AnyRef] )
                .invoke( null, ctx )
            }
          }.getOrElse{
            val buildClasses =
              lib.iterateClasses( compileTarget, classLoader, false )
                .filter(_.getSimpleName == lib.buildClassName)
                .filter(classOf[BaseBuild] isAssignableFrom _)
            if( buildClasses.size == 0 ){
              throw new Exception(
                s"You need to define a class ${lib.buildClassName} extending an appropriate super class in\n"
                + (projectDirectory / lib.buildFileName) ++ "\nbut none found."
              )
            } else if( buildClasses.size > 1 ){
              throw new Exception(
                s"You need to define exactly one class ${lib.buildClassName} extending an appropriate build super class, but multiple found in " + projectDirectory + ":\n" + buildClasses.mkString("\n")
              )
            } else {
              val buildClass = buildClasses.head
              if( !buildClass.getConstructors.exists(_.getParameterTypes.toList == List(classOf[Context])) ){
                throw new Exception(
                  s"Expected class ${lib.buildClassName}(val context: Context), but found different constructor in\n"
                  + projectDirectory ++ "\n"
                  + buildClass ++ "(" ++ buildClass.getConstructors.map(_.getParameterTypes.mkString(", ")).mkString("; ") + ")" )
              }
              buildClass.getConstructors.head.newInstance(managedContext)
            }
          }
      }
    )
    try{
      build.asInstanceOf[BuildInterface]
    } catch {
      case e: ClassCastException if e.getMessage.contains(s"${lib.buildClassName} cannot be cast to cbt.BuildInterface") =>
        throw new Exception(s"Your ${lib.buildClassName} class needs to extend BaseBuild in: "+projectDirectory, e)
    }
  }
  override def triggerLoopFiles = super.triggerLoopFiles ++ managedBuild.triggerLoopFiles
  @deprecated("use finalbuild(File)","")
  override def finalBuild: BuildInterface = finalBuild( context.cwd )
  override def finalBuild( current: File ): BuildInterface = {
    val p = projectDirectory.getCanonicalFile
    val c = current.getCanonicalFile
    if( c == p ) this else managedBuild.finalBuild( current )
  }
}
