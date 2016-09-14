package cbt

import org.wartremover.WartTraverser
import java.io.File

trait WartRemover extends BaseBuild {

  override def scalacOptions =
    super.scalacOptions ++ wartremoverScalacOptions

  private[this] def wartremoverCompilerDependency: String =
    MavenResolver(
      context.cbtHasChanged,
      context.paths.mavenCache,
      mavenCentral).bindOne(
      ScalaDependency("org.wartremover", "wartremover", "1.1.1")
    ).jar.string

  private[this] def wartremoverScalacOptions: Seq[String] =
    Seq("-Xplugin:" ++ wartremoverCompilerDependency) ++
    wartremoverErrorsScalacOptions ++
    wartremoverWarningsScalacOptions ++
    wartremoverExcludedScalacOptions ++
    wartremoverClasspathsScalacOptions

  private[this] def wartremoverErrorsScalacOptions: Seq[String] =
    wartremoverErrors.distinct.map(w => s"-P:wartremover:traverser:${w.className}")

  private[this] def wartremoverWarningsScalacOptions: Seq[String] =
    wartremoverWarnings.distinct
      .filterNot(wartremoverErrors contains _)
      .map(w => s"-P:wartremover:only-warn-traverser:${w.className}")

  private[this] def wartremoverExcludedScalacOptions: Seq[String] =
    wartremoverExcluded.distinct.map(c => s"-P:wartremover:excluded:${c.getAbsolutePath}")
  
  private[this] def wartremoverClasspathsScalacOptions: Seq[String] =
    wartremoverClasspaths.distinct.map(cp => s"-P:wartremover:cp:$cp")

  /** List of Warts that will be reported as compilation errors. */
  def wartremoverErrors: Seq[WartTraverser] = Seq.empty

  /** List of Warts that will be reported as compilation warnings. */
  def wartremoverWarnings: Seq[WartTraverser] = Seq.empty

  /** List of files to be excluded from all checks. */
  def wartremoverExcluded: Seq[File] = Seq.empty

  /** List of classpaths for custom Warts. */
  def wartremoverClasspaths: Seq[String] = Seq.empty
}
