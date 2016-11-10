package cbt
import java.io._
import java.util.concurrent.ConcurrentHashMap
import java.lang._

case class ContextImplementation(
  projectDirectory: File,
  cwd: File,
  argsArray: Array[String],
  enabledLoggersArray: Array[String],
  startCompat: Long,
  cbtHasChangedCompat: Boolean,
  scalaVersionOrNull: String,
  permanentKeys: ConcurrentHashMap[String,AnyRef],
  permanentClassLoaders: ConcurrentHashMap[AnyRef,ClassLoader],
  taskCache: ConcurrentHashMap[AnyRef,AnyRef],
  cache: File,
  cbtHome: File,
  cbtRootHome: File,
  compatibilityTarget: File,
  parentBuildOrNull: BuildInterface
) extends Context