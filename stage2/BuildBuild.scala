package cbt
import java.io.File
import scala.collection.immutable.Seq

class BuildBuild(context: Context) extends Build(context){
  override def dependencies = Seq( CbtDependency()(context.logger) ) ++ super.dependencies
  def managedBuildDirectory: File = lib.realpath( projectDirectory.parent )
  val managedBuild = try{
    val managedContext = context.copy( projectDirectory = managedBuildDirectory )
    val cl = classLoader(context.classLoaderCache)
    logger.composition("Loading build at "+managedContext.projectDirectory)
      cl
        .loadClass(lib.buildClassName)
        .getConstructor(classOf[Context])
        .newInstance(managedContext)
        .asInstanceOf[Build]
  } catch {
    case e: ClassNotFoundException if e.getMessage == lib.buildClassName => 
      throw new Exception("You need to remove the directory or define a class Build in: "+context.projectDirectory)
    case e: Exception =>
      throw new Exception("during build: "+context.projectDirectory, e)
  }
  override def triggerLoopFiles = super.triggerLoopFiles ++ managedBuild.triggerLoopFiles
  override def finalBuild = if( context.projectDirectory == context.cwd ) this else managedBuild.finalBuild
}
