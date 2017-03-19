package cbt
class LazyDependency( _dependency: => Dependency )( implicit logger: Logger, transientCache: java.util.Map[AnyRef, AnyRef], classLoaderCache: ClassLoaderCache ) extends Dependency {
  lazy val dependency = _dependency
  def classLoader = dependency.classLoader
  def dependenciesArray = Array( dependency )
  def exportedClasspathArray = Array()
  def lastModified = dependency.lastModified
  lazy val moduleKey = show
  def show = s"LazyDependency(${dependency.show})"
  override def toString = show
  override def equals( other: Any ) = other match {
    case d: LazyDependency => d.dependency === dependency
    case _                 => false
  }
  def dependencyClasspathArray = dependency.classpath.files.toArray
  def needsUpdateCompat = false
}
