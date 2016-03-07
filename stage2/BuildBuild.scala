package cbt
import java.io.File
import scala.collection.immutable.Seq

class BuildBuild(context: Context) extends Build(context){
  override def dependencies = Seq( CbtDependency(context.logger) ) ++ super.dependencies
  def managedBuildDirectory: File = lib.realpath( projectDirectory.parent )
  val managedBuild = {
    val managedContext = context.copy( cwd = managedBuildDirectory )
    val cl = new cbt.URLClassLoader(
      classpath,
      classOf[BuildBuild].getClassLoader // FIXME: this looks wrong. Should be ClassLoader.getSystemClassLoader but that crashes
    )
    lib.create( lib.buildClassName )( managedContext )( cl ).asInstanceOf[Build]
  }
  override def triggerLoopFiles = super.triggerLoopFiles ++ managedBuild.triggerLoopFiles
  override def finalBuild = managedBuild.finalBuild
}
