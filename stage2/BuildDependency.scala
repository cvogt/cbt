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
final case class DirectoryDependency(context: Context, pathToNestedBuild: String*) extends TriggerLoop{
  def classLoaderCache = context.classLoaderCache
  override def toString = show
  override def show = this.getClass.getSimpleName ++ "(" ++ context.workingDirectory.string ++ ")"
  def moduleKey = this.getClass.getName ++ "("+context.workingDirectory.string+")"
  lazy val logger = context.logger
  override lazy val lib: Lib = new Lib(logger)
  def transientCache = context.transientCache
  private lazy val root = lib.loadRoot( context )
  lazy val dependency: Dependency = {
    def selectNestedBuild( build: Dependency, names: Seq[String], previous: Seq[String] ): Dependency = {
      names.headOption.map{ name =>
        if( lib.taskNames(build.getClass).contains(name) ){
          val method = build.getClass.getMethod(name)
          val returnType = method.getReturnType
          if( classOf[Dependency] isAssignableFrom returnType ){
            selectNestedBuild(
              method.invoke(build).asInstanceOf[Dependency],
              names.tail,
              previous :+ name
            )
          } else {
            throw new RuntimeException( s"Expected subtype of Dependency, found $returnType for  " + previous.mkString(".") + " in " + show )
          }
        } else {
          throw new RuntimeException( (previous :+ name).mkString(".") + " not found in " + show )
        }
      }.getOrElse( build )
    }
    selectNestedBuild( root.finalBuild(context.workingDirectory), pathToNestedBuild, Nil )
  }
  def exportedClasspath = ClassPath()
  def dependencies = Seq(dependency)
  def triggerLoopFiles = root.triggerLoopFiles
  def lastModified = dependency.lastModified
  def targetClasspath = ClassPath()
}
/*
case class DependencyOr(first: DirectoryDependency, second: JavaDependency) extends ProjectProxy with DirectoryDependencyBase{
  val isFirst = new File(first.projectDirectory).exists
  def triggerLoopFiles = if(isFirst) first.triggerLoopFiles else Seq()
  protected val delegate = if(isFirst) first else second
}
*/