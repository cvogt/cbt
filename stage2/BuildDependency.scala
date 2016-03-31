package cbt
import java.io.File
import scala.collection.immutable.Seq
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
trait TriggerLoop extends Dependency{
  def triggerLoopFiles: Seq[File]
}
/** You likely want to use the factory method in the BasicBuild class instead of this. */
case class BuildDependency(context: Context) extends TriggerLoop{
  override def show = this.getClass.getSimpleName ++ "(" ++ context.cwd.string ++ ")"
  final override lazy val logger = context.logger
  final override lazy val lib: Lib = new Lib(logger)
  private val root = lib.loadRoot( context.copy(args=Seq()) )
  lazy val build = root.finalBuild
  def exportedClasspath = ClassPath(Seq())
  def exportedJars = Seq()
  def dependencies = Seq(build)
  def triggerLoopFiles = root.triggerLoopFiles
  override final val needsUpdate = build.needsUpdate
  def targetClasspath = ClassPath(Seq())
}
/*
case class DependencyOr(first: BuildDependency, second: JavaDependency) extends ProjectProxy with BuildDependencyBase{
  val isFirst = new File(first.context.cwd).exists
  def triggerLoopFiles = if(isFirst) first.triggerLoopFiles else Seq()
  protected val delegate = if(isFirst) first else second
}
*/