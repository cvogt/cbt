package cbt
import java.nio.file._

class BuildBuild(context: Context) extends BasicBuild(context){
  override def dependencies =
    super.dependencies :+ context.cbtDependency
  def managedBuildDirectory: File = lib.realpath( projectDirectory.parent )
  private object managedBuildCache extends Cache[BuildInterface]
  def managedBuild = managedBuildCache{
    try{
      val managedContext = context.copy(
        projectDirectory = managedBuildDirectory,
        parentBuild=Some(this)
      )
      val managedBuildFile = projectDirectory++"/build.scala"
      logger.composition("Loading build at "++managedContext.projectDirectory.toString)
      (
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
                val checkoutDirectory = new GitDependency(base, hash).checkout
                val build = new BasicBuild( context.copy( projectDirectory = checkoutDirectory ++ "/nailgun_launcher" ) )
                val cl = build
                  .classLoader(classLoaderCache)
                // Note: cbt can't use an old version of itself for building,
                // otherwise we'd have to recursively build all versions since
                // the beginning. Instead CBT always needs to build the pure Java
                // Launcher in the checkout with itself and then run it via reflection.
                cl
                  .loadClass( "cbt.NailgunLauncher" )
                  .getMethod( "getBuild", classOf[AnyRef] )
                  .invoke( null, managedContext.copy(cbtHome=checkoutDirectory) )
              }
            }.getOrElse{
              classLoader(context.classLoaderCache)
                .loadClass(lib.buildClassName)
                .getConstructors.head
                .newInstance(managedContext)
            }
        } else {
          new BasicBuild(managedContext)
        }
      ).asInstanceOf[BuildInterface]
    } catch {
      case e: ClassNotFoundException if e.getMessage == lib.buildClassName => 
        throw new Exception("You need to remove the directory or define a class Build in: "+context.projectDirectory)
      case e: Exception =>
        throw new Exception("during build: "+context.projectDirectory, e)
    }
  }
  override def triggerLoopFiles = super.triggerLoopFiles ++ managedBuild.triggerLoopFiles
  override def finalBuild: BuildInterface = if( context.projectDirectory == context.cwd ) this else managedBuild.finalBuild
}
