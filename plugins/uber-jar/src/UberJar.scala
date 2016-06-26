package cbt

import java.io.File
import java.nio.file.{FileSystems, Files, Path}
import java.util.jar.JarFile

trait UberJar extends BaseBuild {

  final def uberJar: ExitCode = {
    System.err.println("Creating uber jar...")
    new UberJarLib(logger).create(target, classpath, uberJarMainClass, uberJarName)
    System.err.println(lib.green("Creating uber jar - DONE"))
    ExitCode.Success
  }

  def uberJarMainClass: Option[String] = Some(runClass)

  def uberJarName: String = projectName + ".jar"

}

class UberJarLib(logger: Logger) {
  private val (jarFileMatcher, excludeFileMatcher) = {
    val fs = FileSystems.getDefault
    (fs.getPathMatcher("glob:**.jar"), fs.getPathMatcher("glob:**{.RSA,.DSA,.SF,.MF,META-INF}"))
  }
  private val log: String => Unit = logger.log("uber-jar", _)
  private val lib = new cbt.Lib(logger)

  /**
    * Creates uber jar for given build.
    *
    * @param target        build's target directory
    * @param classpath     build's classpath
    * @param mainClass     optional main class
    * @param jarName       name of resulting jar file
    */
  def create(target: File,
             classpath: ClassPath,
             mainClass: Option[String],
             jarName: String): Unit = {
    log(s"Classpath is: $classpath")
    log(s"Target directory is: $target")
    log(s"Jar name is: $jarName")
    mainClass foreach (c => log(s"Main class is is: $c"))

    val (jars, dirs) = classpath.files partition (f => jarFileMatcher.matches(f.toPath))
    log(s"Found ${jars.length} jar dependencies: \n ${jars mkString "\n"}")
    log(s"Found ${dirs.length} directories in classpath: \n ${dirs mkString "\n"}")

    log("Extracting jars...")
    val extractedJarsRoot = extractJars(jars.distinct)(log).toFile
    log("Extracting jars - DONE")

    log("Writing jar file...")
    val uberJarPath = target.toPath.resolve(jarName)
    val uberJar = lib.jarFile(uberJarPath.toFile, dirs :+ extractedJarsRoot, mainClass) getOrElse {
        throw new Exception("Jar file wasn't created!")
      }
    log("Writing jar file - DONE")

    System.err.println(lib.green(s"Uber jar created. You can grab it at $uberJar"))
  }

  /**
    * Extracts jars, and writes them on disk. Returns root directory of extracted jars
    * TODO: in future we probably should save extracted jars in target directory, to reuse them on second run
    *
    * @param jars list of *.jar files
    * @param log  logger
    * @return root directory of extracted jars
    */
  private def extractJars(jars: Seq[File])(log: String => Unit): Path = {
    val destDir = {
      val path = Files.createTempDirectory("unjars")
      path.toFile.deleteOnExit()
      log(s"Extracted jars directory: $path")
      path
    }
    jars foreach { jar => extractJar(jar, destDir)(log) }
    destDir
  }

  /**
    * Extracts content of single jar file to destination directory.
    * When extracting jar, if same file already exists, we skip(don't write) this file.
    * TODO: maybe skipping duplicates is not best strategy. Figure out duplicate strategy.
    *
    * @param jarFile jar file to extract
    * @param destDir destination directory
    * @param log     logger
    */
  private def extractJar(jarFile: File, destDir: Path)(log: String => Unit): Unit = {
    log(s"Extracting jar: $jarFile")
    val jar = new JarFile(jarFile)
    val enumEntries = jar.entries
    while (enumEntries.hasMoreElements) {
      val entry = enumEntries.nextElement()
      //        log(s"Entry name: ${entry.getName}")
      val entryPath = destDir.resolve(entry.getName)
      if (excludeFileMatcher.matches(entryPath)) {
        log(s"Excluded file ${entryPath.getFileName} from jar: $jarFile")
      } else {
        val exists = Files.exists(entryPath)
        if (entry.isDirectory) {
          if (!exists) {
            Files.createDirectory(entryPath)
            //              log(s"Created directory: $entryPath")
          }
        } else {
          if (exists) {
            log(s"File $entryPath already exists, skipping.")
          } else {
            val is = jar.getInputStream(entry)
            Files.copy(is, entryPath)
            is.close()
            //              log(s"Wrote file: $entryPath")
          }
        }
      }
    }
  }

}
