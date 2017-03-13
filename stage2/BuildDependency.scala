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
/** You likely want to use the factory method in the BasicBuild class instead of this. */
object DirectoryDependency{
  def apply(context: Context, pathToNestedBuild: String*): BuildInterface = {
    val lib: Lib = new Lib(context.logger)

    // TODO: move this into finalBuild probably
    // TODO: unify this with lib.callReflective
    def selectNestedBuild( build: BuildInterface, names: Seq[String], previous: Seq[String] ): BuildInterface = {
      names.headOption.map{ name =>
        if( lib.taskNames(build.getClass).contains(name) ){
          val method = build.getClass.getMethod(name)
          val returnType = method.getReturnType
          if( classOf[BuildInterface] isAssignableFrom returnType ){
            selectNestedBuild(
              method.invoke(build).asInstanceOf[BuildInterface],
              names.tail,
              previous :+ name
            )
          } else {
            throw new RuntimeException(
              s"Expected subtype of BuildInterface, found $returnType for  " + previous.mkString(".") + " in " + build
            )
          }
        } else {
          throw new RuntimeException( (previous :+ name).mkString(".") + " not found in " + build )
        }
      }.getOrElse( build )
    }
    selectNestedBuild(
      lib.loadRoot( context ).finalBuild(context.workingDirectory),
      pathToNestedBuild,
      Nil
    )
  }
}
/*
case class DependencyOr(first: DirectoryDependency, second: JavaDependency) extends ProjectProxy with DirectoryDependencyBase{
  val isFirst = new File(first.projectDirectory).exists
  protected val delegate = if(isFirst) first else second
}
*/
