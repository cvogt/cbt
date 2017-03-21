package cbt

import java.io.File
import java.nio.file.{FileSystems, Files, Path}
import java.nio.file.Files._
import java.util.jar.JarFile

trait UberJar extends PackageJars {
  lazy val uberJarLib = new UberJarLib(lib, logger.log("uberjar", _))

  def uberJarName: String = name ++ "-" ++ version ++ "-uberjar"
  def uberJarMainClass: Option[String] = None // to avoid asking interactively
  def uberJar: Option[File] =
    uberJarLib.create( target / uberJarName ++ ".jar", classpath, uberJarMainClass )
}

class UberJarLib(lib: cbt.Lib, log: String => Unit) {
  /**
    * Bundles the given classpath into a single jar (uber jar / fat jar).
    *
    * @param jarFile       name of jar to create
    * @param classpath     class path to bundle into the jar
    */
  def create( jarFile: File, classpath: ClassPath, mainClass: Option[String] ): Option[File] = {
    System.err.println("Creating uber jar...")

    log(s"Classpath: ${classpath.string}")
    log( mainClass.map("Main class is: "+_).getOrElse("no Main class") )

    val (dirs, jars) = classpath.files.partition(_.isDirectory)
    log(s"Found ${jars.length} jars in classpath: \n${jars mkString "\n"}")
    log(s"Found ${dirs.length} directories in classpath: \n${dirs mkString "\n"}")

    val extracted = Files.createTempDirectory("unjars").toFile.getCanonicalFile

    log("Extracting jars to $extracted")
    jars.foreach( extractJar(_, extracted.toPath) )
    log("Extracting jars - DONE")

    log("Writing jar file...")
    val uberJar = lib.createJar(jarFile, dirs :+ extracted, mainClass=mainClass)
    log("Writing uber jar - DONE")

    lib.deleteRecursive( extracted )

    System.err.println(lib.green("Creating uber jar - DONE"))

    uberJar
  }


  /**
    * Extracts contents of single jar file into a destination directory.
    * When extracting the jar, if the same file already exists, skips the file.
    * TODO: allow custom strategies to resolve duplicate files, not only skipping.
    * TODO: do not hard code excluded files.
    *
    * @param jarFile jar file to extract
    */
  private def extractJar(jarFile: File, destination: Path): Unit = {
    val excludeFilter: File => Boolean = {
      f => f.getName == "META-INF" || Seq(".RSA",".DSA",".SF",".MF").exists(f.getName.endsWith)
    }

    log(s"Extracting $jarFile")
    val jar = new JarFile(jarFile)

    import collection.JavaConverters._
    val entries = jar.entries.asScala.filterNot(_.isDirectory).toVector
    val paths = entries.map( e => destination.resolve(e.getName) ) // JarFile.entry is the full path, not just file name

    paths.map( _.getParent ).distinct.foreach( createDirectories(_) )

    (entries zip paths).foreach{
      case( entry, path ) =>
        if( excludeFilter(path.toFile) ){
          log(s"Excluded $path")
        } else if (exists(path)) {
          log(s"Already exists, skipping $path")
        } else {
          val is = jar.getInputStream(entry)
          try{ Files.copy(is, path) }
          finally{ is.close }
        }
    }
  }
}
