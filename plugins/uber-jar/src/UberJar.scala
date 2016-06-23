package cbt

import java.io.{File, FileOutputStream}
import java.nio.file._
import java.util.jar
import java.util.jar.JarOutputStream

import cbt.uberjar._

import scala.util.{Failure, Success, Try}

trait UberJar extends BaseBuild {

  private val log: String => Unit = logger.log("uber-jar", _)

  final def uberJar: ExitCode = {
    // we need compile first to produce target directory
    compile
    System.err.println("Creating uber jar...")
    UberJar.createUberJar(
      targetDir = target,
      compilerTarget = compileTarget,
      classpath = classpath,
      mainClass = uberJarMainClass,
      jarName = uberJarName
    )(log) match {
      case Success(_) =>
        System.err.println("Creating uber jar - DONE")
        ExitCode.Success
      case Failure(e) =>
        System.err.println(s"Failed to create uber jar, cause: $e")
        ExitCode.Failure
    }
  }

  def uberJarMainClass: Option[String] = Some(runClass)

  def uberJarName: String = projectName

}

object UberJar extends JarUtils {
  import TryWithResources._

  /**
    * Creates uber jar for given build.
    * Uber jar construction steps:
    * 1. create jar file with our customized MANIFEST.MF
    * 2. write files from `compilerTarget` to jar file
    * 3. get all jars from `classpath`
    * 4. extract all jars, filter out their MANIFEST.MF and signatures files
    * 5. write content of all jars to target jar file
    * 6. Finalize everything, and return `ExitCode`
    *
    * @param targetDir build's target directory
    * @param compilerTarget directory where compiled classfiles are
    * @param classpath build's classpath
    * @param mainClass main class name(optional)
    * @param jarName name of resulting jar file
    * @param log logger
    * @return `ExitCode.Success` if uber jar created and `ExitCode.Failure` otherwise
    */
  def createUberJar(targetDir: File,
                    compilerTarget: File,
                    classpath: ClassPath,
                    mainClass: Option[String],
                    jarName: String
                   )(log: String => Unit): Try[Unit] = {
    val targetPath = targetDir.toPath
    log(s"Target directory is: $targetPath")
    log(s"Compiler targer directory is: $compilerTarget")
    log(s"Classpath is: $classpath")
    mainClass foreach (c => log(s"Main class is is: $c"))

    val jarPath = {
      log("Creating jar file...")
      val validJarName = if (jarName.endsWith("*.jar")) jarName else jarName + ".jar"
      log(s"Jar name is: $validJarName")
      val path = targetPath.resolve(validJarName)
      Files.deleteIfExists(path)
      Files.createFile(path)
      log("Creating jar file - DONE")
      path
    }
    withCloseable(new JarOutputStream(new FileOutputStream(jarPath.toFile), createManifest(mainClass))) { out =>
      writeTarget(compilerTarget, out)(log)

      // it will contain all jar files, including jars, that cbt depend on! Not good!
      val jars = classpath.files filter (f => jarFileMatcher.matches(f.toPath))
      log(s"Found ${jars.length} jar dependencies: \n ${jars mkString "\n"}")
      writeExtractedJars(jars, targetPath, out)(log)

      // TODO: make it in try-catch-finally style
      out.close()
      System.err.println(s"Uber jar created. You can grab it at $jarPath")
    }
  }

  /**
    * Writes three attributes to manifest file:
    *
    *  Main-Class: classname
    *  Manifest-Version: 1.0
    *  Created-By: java.runtime.version
    *
    * @param mainClass optional main class
    * @return mainifest for jar
    */
  private def createManifest(mainClass: Option[String]): jar.Manifest = {
    val m = new jar.Manifest()
    m.getMainAttributes.putValue("Manifest-Version", "1.0")
    val createdBy = Option(System.getProperty("java.runtime.version")) getOrElse "1.7.0_06 (Oracle Corporation)"
    m.getMainAttributes.putValue("Created-By", createdBy)
    mainClass foreach { className =>
      m.getMainAttributes.putValue("Main-Class", className)
    }
    m
  }

  private def writeTarget(compilerTargetDir: File, out: JarOutputStream)(log: String => Unit): Unit = {
    log("Writing target directory...")
    writeFilesToJar(compilerTargetDir.toPath, out)(log)
    log("Writing target directory - DONE")
  }

  private def writeExtractedJars(jars: Seq[File], targetDir: Path, out: JarOutputStream)(log: String => Unit): Unit = {
    log("Extracting jars")
    val extractedJarsRoot = extractJars(jars)(log)
    log("Extracting jars - DONE")

    log("Writing dependencies...")
    writeFilesToJar(extractedJarsRoot, out)(log)
    log("Writing dependencies - DONE")
  }

}
