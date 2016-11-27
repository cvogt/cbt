package cbt
import java.io._
import java.lang._

case class ContextImplementation(
  projectDirectory: File,
  cwd: File,
  argsArray: Array[String],
  enabledLoggersArray: Array[String],
  startCompat: Long,
  cbtHasChangedCompat: Boolean,
  scalaVersionOrNull: String,
  persistentCache: java.util.Map[AnyRef,AnyRef],
  transientCache: java.util.Map[AnyRef,AnyRef],
  cache: File,
  cbtHome: File,
  cbtRootHome: File,
  compatibilityTarget: File,
  parentBuildOrNull: BuildInterface
) extends Context