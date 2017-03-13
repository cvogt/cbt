package cbt
import java.io._

class ContextImplementation(
  override val workingDirectory: File,
  override val cwd: File,
  override val argsArray: Array[String],
  override val enabledLoggersArray: Array[String],
  override val start: Long,
  override val cbtLastModified: Long,
  override val scalaVersionOrNull: String,
  override val persistentCache: java.util.Map[AnyRef,AnyRef],
  override val transientCache: java.util.Map[AnyRef,AnyRef],
  override val cache: File,
  override val cbtHome: File,
  override val cbtRootHome: File,
  override val compatibilityTarget: File,
  override val parentBuildOrNull: BuildInterface,
  override val loop: Boolean
) extends Context{
  @deprecated("this method is replaced by workingDirectory","")
  def projectDirectory = workingDirectory
  @deprecated("this method is replaced by cbtLastModified","")
  def cbtHasChangedCompat = true
  @deprecated("this method is replaced by start","")
  def startCompat = start
  @deprecated("this methods is replaced by persistentCache","")
  def permanentKeys = throw new IncompatibleCbtVersionException("You need to upgrade your CBT version in this module. The Context field permanentClassLoaders is no longer supported.");
  @deprecated("this methods is replaced by persistentCache","")
  def permanentClassLoaders = throw new IncompatibleCbtVersionException("You need to upgrade your CBT version in this module. The Context field permanentClassLoaders is no longer supported.");
}
