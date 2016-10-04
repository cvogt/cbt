package cbt
import java.io._
import java.util.concurrent.ConcurrentHashMap
import java.lang._

case class ContextImplementation(
  projectDirectory: File,
  cwd: File,
  propsMap: java.util.Map[String, String],
  argsArray: Array[String],
  enabledLoggersArray: Array[String],
  startCompat: Long,
  cbtHasChangedCompat: Boolean,
  versionOrNull: String,
  scalaVersionOrNull: String,
  permanentKeys: ConcurrentHashMap[String,AnyRef],
  permanentClassLoaders: ConcurrentHashMap[AnyRef,ClassLoader],
  cache: File,
  cbtHome: File,
  cbtRootHome: File,
  compatibilityTarget: File,
  parentBuildOrNull: BuildInterface
) extends Context
