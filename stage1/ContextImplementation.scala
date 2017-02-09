package cbt
import java.io._
import java.lang._

class ContextImplementation(
  val projectDirectory: File,
  val cwd: File,
  val argsArray: Array[String],
  val enabledLoggersArray: Array[String],
  val startCompat: Long,
  val cbtHasChangedCompat: Boolean,
  val scalaVersionOrNull: String,
  val persistentCache: java.util.Map[AnyRef,AnyRef],
  val transientCache: java.util.Map[AnyRef,AnyRef],
  val cache: File,
  val cbtHome: File,
  val cbtRootHome: File,
  val compatibilityTarget: File,
  val parentBuildOrNull: BuildInterface
) extends Context
