package cbt.uberjar

import java.io.File
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.util.jar.{JarFile, JarOutputStream}
import java.util.zip.{ZipEntry, ZipException}

private[cbt] trait JarUtils {

  protected val (pathSeparator, jarFileMatcher, excludeFileMatcher) = {
    val fs = FileSystems.getDefault
    (fs.getSeparator, fs.getPathMatcher("glob:**.jar"), fs.getPathMatcher("glob:**{.RSA,.DSA,.SF,.MF}"))
  }

  /**
    * If `root` is directory: writes content of directory to jar with original mapping
    * If `root` is file: writes this file to jar
    * @param root parent directory, with content should go to jar, or file to be written
    * @param out jar output stream
    * @param log logger
    * @return returns `root`
    */
  protected def writeFilesToJar(root: Path, out: JarOutputStream)(log: String => Unit): Path = {
    Files.walkFileTree(root, new SimpleFileVisitor[Path]() {
      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
        // when we put part of compiled classes, zip already contains some entries. We should not duplicate them.
        try {
          out.putNextEntry(new ZipEntry(root.relativize(file).toString))
          Files.copy(file, out)
          out.closeEntry()
        } catch {
          case e: ZipException => log(s"Failed to add entry, skipping cause: $e")
        }
        FileVisitResult.CONTINUE
      }

      override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
        // when we put part compiled classes, zip already contains some entries. We should not duplicate them.
        try {
          out.putNextEntry(new ZipEntry(root.relativize(dir).toString + pathSeparator))
          out.closeEntry()
        } catch {
          case e: ZipException => log(s"Failed to add entry, skipping cause: $e")
        }
        FileVisitResult.CONTINUE
      }
    })
  }

  /**
    * Extracts jars, and writes them on disk. Returns root directory of extracted jars
    * TODO: in future we probably should save extracted jars in target directory, to reuse them on second run
    * @param jars list of *.jar files
    * @param log logger
    * @return root directory of extracted jars
    */
  protected def extractJars(jars: Seq[File])(log: String => Unit): Path = {
    val destDir = {
      val path = Files.createTempDirectory("unjars")
      path.toFile.deleteOnExit()
      log(s"Unjars directory: $path")
      path
    }
    jars foreach { jar => extractJar(jar, destDir)(log) }
    destDir
  }

  /**
    * Extracts content of single jar file to destination directory.
    * When extracting jar, if same file already exists, we skip(don't write) this file.
    * TODO: maybe skipping duplicates is not best strategy. Figure out duplicate strategy.
    * @param jarFile jar file to extract
    * @param destDir destination directory
    * @param log logger
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
        if (Files.exists(entryPath)) {
          log(s"File $entryPath already exists, skipping.")
        } else {
          if (entry.isDirectory) {
            Files.createDirectory(entryPath)
            //              log(s"Created directory: $entryPath")
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
