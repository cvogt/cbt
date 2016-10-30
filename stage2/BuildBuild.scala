package cbt
import java.nio.file._

trait BuildBuild extends BaseBuild{
  private final val managedContext = context.copy(
    projectDirectory = managedBuildDirectory,
    parentBuild=Some(this)
  )

  object plugins{
    final lazy val scalaTest = DirectoryDependency( managedContext.cbtHome ++ "/plugins/scalatest" )
    final lazy val scalaJs   = DirectoryDependency( managedContext.cbtHome ++ "/plugins/scalajs" )
    final lazy val scalariform = DirectoryDependency( managedContext.cbtHome ++ "/plugins/scalariform" )
    final lazy val scalafmt = DirectoryDependency( managedContext.cbtHome ++ "/plugins/scalafmt" )
    final lazy val wartremover = DirectoryDependency( managedContext.cbtHome ++ "/plugins/wartremover" )
  }

  override def dependencies =
    super.dependencies :+ context.cbtDependency
  def managedBuildDirectory: java.io.File = lib.realpath( projectDirectory.parent )
  private object managedBuildCache extends Cache[BuildInterface]
  def managedBuild = managedBuildCache{
    val managedBuildFile = projectDirectory++"/build.scala"
    logger.composition("Loading build at "++managedContext.projectDirectory.toString)
    val build = (
      if(managedBuildFile.exists){
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
              dep.classLoader(classLoaderCache)
                .loadClass( "cbt.NailgunLauncher" )
                .getMethod( "getBuild", classOf[AnyRef] )
                .invoke( null, ctx )
            }
          }.getOrElse{
            try{
              classLoader(context.classLoaderCache)
                .loadClass(lib.buildClassName)
                .getConstructors.head
                .newInstance(managedContext)
              } catch {
                case e: ClassNotFoundException if e.getMessage == lib.buildClassName => 
                  throw new Exception("You need to define a class Build in build.scala in: "+context.projectDirectory)
              }
          }
      } else if( projectDirectory.listFiles.exists( _.getName.endsWith(".scala") ) ){
        throw new Exception(
          "No file build.scala (lower case) found in " ++ projectDirectory.getPath
        )
      } else if( projectDirectory.getParentFile.getName == "build" ){
        new BasicBuild( managedContext ) with BuildBuild
      } else {
        new BasicBuild( managedContext )
      }
    )
    try{
      build.asInstanceOf[BuildInterface]
    } catch {
      case e: ClassCastException if e.getMessage.contains("Build cannot be cast to cbt.BuildInterface") =>
        throw new Exception("Your Build class needs to extend BaseBuild in: "+context.projectDirectory, e)
    }
  }
  override def triggerLoopFiles = super.triggerLoopFiles ++ managedBuild.triggerLoopFiles
  override def finalBuild: BuildInterface = if( context.projectDirectory == context.cwd ) this else managedBuild.finalBuild
}
