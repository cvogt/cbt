package cbt
import java.io._
import java.util.concurrent.ConcurrentHashMap
import scala.collection.immutable.Seq
import java.lang._

case class ContextImplementation(
  projectDirectory: File,
  cwd: File,
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
  compatibilityTarget: File,
  parentBuildOrNull: BuildInterface
) extends Context