package cbt
import java.io._
import java.nio._
import java.nio.file._
import java.util.concurrent.ConcurrentHashMap
import java.lang._

case class ContextImplementation(
  projectDirectory: Path,
  cwd: Path,
  argsArray: Array[String],
  enabledLoggersArray: Array[String],
  startCompat: Long,
  cbtHasChangedCompat: Boolean,
  versionOrNull: String,
  scalaVersionOrNull: String,
  permanentKeys: ConcurrentHashMap[String,AnyRef],
  permanentClassLoaders: ConcurrentHashMap[AnyRef,ClassLoader],
  cache: Path,
  cbtHome: Path,
  cbtRootHome: Path,
  compatibilityTarget: Path,
  parentBuildOrNull: BuildInterface
) extends Context