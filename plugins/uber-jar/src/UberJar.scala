package cbt

import java.io.File
import java.nio.file.{Files, Path}

import cbt.uberjar._

trait UberJar extends BaseBuild {

  final def uberJar: ExitCode = {
    System.err.println("Creating uber jar...")
    new UberJarLib(logger).create(target, compileTarget, classpath, uberJarMainClass, uberJarName)
    System.err.println("Creating uber jar - DONE")
    ExitCode.Success
  }

  def uberJarMainClass: Option[String] = Some(runClass)

  def uberJarName: String = projectName

}

class UberJarLib(logger: Logger) extends JarUtils {
  private val log: String => Unit = logger.log("uber-jar", _)
  private val lib = new cbt.Lib(logger)

  /**
    * Creates uber jar for given build.
    *
    * @param target        build's target directory
    * @param compileTarget directory where compiled classfiles are
    * @param classpath     build's classpath
    * @param jarName       name of resulting jar file
    */
  def create(target: File,
             compileTarget: File,
             classpath: ClassPath,
             mainClass: Option[String],
             jarName: String): Unit = {
    log(s"Compiler target directory is: $compileTarget")
    log(s"Classpath is: $classpath")
    log(s"Target directory is: $target")
    mainClass foreach (c => log(s"Main class is is: $c"))

    log("Creating far file...")
    val file = createJarFile(target.toPath, jarName)
    log("Creating far file - DONE")

    val jars = classpath.files filter (f => jarFileMatcher.matches(f.toPath))
    log(s"Found ${jars.length} jar dependencies: \n ${jars mkString "\n"}")

    log("Extracting jars...")
    val extractedJarsRoot = extractJars(jars.distinct)(log).toFile
    log("Extracting jars - DONE")

    log("Writing jar file...")
    val optOutput = lib.jarFile(file, Seq(compileTarget, extractedJarsRoot), mainClass)
    log("Writing jar file - DONE")

    optOutput foreach { uberJar =>
      System.err.println(s"Uber jar created. You can grab it at $uberJar")
    }

  }
}
