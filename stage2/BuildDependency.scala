package cbt
import java.io.File
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
  def triggerLoopFiles: Seq[File]
}
/** You likely want to use the factory method in the BasicBuild class instead of this. */
final case class DirectoryDependency(context: Context) extends TriggerLoop{
  def classLoaderCache = context.classLoaderCache
  override def toString = show
  override def show = this.getClass.getSimpleName ++ "(" ++ context.workingDirectory.string ++ ")"
  def moduleKey = this.getClass.getName ++ "("+context.workingDirectory.string+")"
  lazy val logger = context.logger
  override lazy val lib: Lib = new Lib(logger)
  def transientCache = context.transientCache
  private lazy val root = lib.loadRoot( context.copy(args=Seq()) )
  lazy val build = root.finalBuild
  def exportedClasspath = ClassPath()
  def dependencies = Seq(build)
  def triggerLoopFiles = root.triggerLoopFiles
  def lastModified = build.lastModified
  def targetClasspath = ClassPath()
}
/*
case class DependencyOr(first: DirectoryDependency, second: JavaDependency) extends ProjectProxy with DirectoryDependencyBase{
  val isFirst = new File(first.projectDirectory).exists
  def triggerLoopFiles = if(isFirst) first.triggerLoopFiles else Seq()
  protected val delegate = if(isFirst) first else second
}
*/