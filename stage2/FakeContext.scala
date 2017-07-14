package cbt

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.{lang, util}

class FakeContext(stage2Args: Stage2Args) extends Context {
  override def cbtRootHome(): File = stage2Args.cbtHome

  override def projectDirectory(): File = stage2Args.cbtHome

  override def permanentKeys(): ConcurrentHashMap[String, AnyRef] = new ConcurrentHashMap

  override def parentBuildOrNull(): BuildInterface = null

  override def cbtLastModified(): Long = stage2Args.stage2LastModified

  override def persistentCache(): util.Map[AnyRef, AnyRef] = stage2Args.persistentCache

  override def start(): Long = 0

  override def transientCache(): util.Map[AnyRef, AnyRef] = stage2Args.transientCache

  override def argsArray(): Array[String] = Array.empty

  override def cache(): File = stage2Args.cache

  override def compatibilityTarget(): File = stage2Args.compatibilityTarget

  override def startCompat(): lang.Long = new lang.Long(0)

  override def enabledLoggersArray(): Array[String] = Array.empty

  override def cbtHasChangedCompat(): lang.Boolean = false

  override def cwd(): File = stage2Args.cwd

  override def permanentClassLoaders(): ConcurrentHashMap[AnyRef, ClassLoader] = new ConcurrentHashMap()

  override def scalaVersionOrNull(): String = null

  override def cbtHome(): File = stage2Args.cbtHome
}

object FakeContext {
  def apply(stage2Args: Stage2Args): FakeContext = new FakeContext(stage2Args)
}
