package cbt
import java.nio._
import java.nio.file._
/*
sealed abstract class ProjectProxy extends Ha{
  protected def delegate: ProjectMetaData
  def artifactId: String = delegate.artifactId
  def groupId: String = delegate.groupId
  def version: String = delegate.version
  def exportedClasspath = delegate.exportedClasspath
  def dependencies = Seq(delegate)
}
*/
trait TriggerLoop extends DependencyImplementation{
  final def triggerLoopFilesArray = triggerLoopFiles.toArray
  def triggerLoopFiles: Seq[Path]
}
/** You likely want to use the factory method in the BasicBuild class instead of this. */
case class DirectoryDependency(context: Context) extends TriggerLoop{
  override def show = this.getClass.getSimpleName ++ "(" ++ context.projectDirectory.string ++ ")"
  final override lazy val logger = context.logger
  final override lazy val lib: Lib = new Lib(logger)
  private val root = lib.loadRoot( context.copy(args=Seq()) )
  lazy val build = root.finalBuild
  def exportedClasspath = ClassPath()
  def dependencies = Seq(build)
  def triggerLoopFiles = root.triggerLoopFiles
  override final val needsUpdate = build.needsUpdate
  def targetClasspath = ClassPath()
}
/*
case class DependencyOr(first: DirectoryDependency, second: JavaDependency) extends ProjectProxy with DirectoryDependencyBase{
  val isFirst = new File(first.context.projectDirectory).exists
  def triggerLoopFiles = if(isFirst) first.triggerLoopFiles else Seq()
  protected val delegate = if(isFirst) first else second
}
*/