package cbt.uberjar

import java.io.File
import java.nio.file._
import java.util.jar.JarFile

private[cbt] trait JarUtils {

  protected val (jarFileMatcher, excludeFileMatcher) = {
    val fs = FileSystems.getDefault
    (fs.getPathMatcher("glob:**.jar"), fs.getPathMatcher("glob:**{.RSA,.DSA,.SF,.MF,META-INF}"))
  }

  protected def createJarFile(parent: Path, name: String): File = {
    val path = parent.resolve(validJarName(name))
    Files.deleteIfExists(path)
    Files.createFile(path)
    path.toFile
  }

  private def validJarName(name: String) = if (name.endsWith(".jar")) name else name + ".jar"

  /**
    * Extracts jars, and writes them on disk. Returns root directory of extracted jars
    * TODO: in future we probably should save extracted jars in target directory, to reuse them on second run
    *
    * @param jars list of *.jar files
    * @param log  logger
    * @return root directory of extracted jars
    */
  protected def extractJars(jars: Seq[File])(log: String => Unit): Path = {
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
