package cbt

import cbt.paths._

import java.io._
import java.lang.reflect.InvocationTargetException
import java.net._
import java.nio.file._
import java.nio.file.attribute.FileTime
import javax.tools._
import java.security._
import java.util._
import javax.xml.bind.annotation.adapters.HexBinaryAdapter

import scala.collection.immutable.Seq

// CLI interop
case class ExitCode(integer: Int)
object ExitCode{
  val Success = ExitCode(0)
  val Failure = ExitCode(1)
}

object CatchTrappedExitCode{
  def unapply(e: Throwable): Option[ExitCode] = {
    Option(e) flatMap {
      case i: InvocationTargetException => unapply(i.getTargetException)
      case e: TrappedExitCode => None// Some( ExitCode(e.exitCode) )
      case _ => None
    }
  }
}

case class Context( cwd: File, args: Seq[String], logger: Logger, cbtHasChanged: Boolean, classLoaderCache: ClassLoaderCache )

class BaseLib{
  def realpath(name: File) = new File(Paths.get(name.getAbsolutePath).normalize.toString)
}

class Stage1Lib( val logger: Logger ) extends BaseLib{
  lib =>
  implicit val implicitLogger: Logger = logger

  def scalaMajorVersion(scalaMinorVersion: String) = scalaMinorVersion.split("\\.").take(2).mkString(".")

  // ========== file system / net ==========

  def array2hex(padTo: Int, array: Array[Byte]): String = {
    val hex = new java.math.BigInteger(1, array).toString(16)
    ("0" * (padTo-hex.size)) ++ hex
  }
  def md5( bytes: Array[Byte] ): String = array2hex(32, MessageDigest.getInstance("MD5").digest(bytes))
  def sha1( bytes: Array[Byte] ): String = array2hex(40, MessageDigest.getInstance("SHA-1").digest(bytes))

  def red(string: String) = scala.Console.RED++string++scala.Console.RESET
  def blue(string: String) = scala.Console.BLUE++string++scala.Console.RESET
  def green(string: String) = scala.Console.GREEN++string++scala.Console.RESET

  def download(url: URL, target: File, sha1: Option[String]): Boolean = {
    if( target.exists ){
      true
    } else {
      val incomplete = Paths.get( target.string ++ ".incomplete" );
      val connection = url.openConnection.asInstanceOf[HttpURLConnection]
      if(connection.getResponseCode != HttpURLConnection.HTTP_OK){
        logger.resolver(blue("not found: ") ++ url.string)
        false
      } else {
        logger.resolver(blue("downloading ") ++ url.string)
        logger.resolver(blue("to ") ++ target.string)
        target.getParentFile.mkdirs
        val stream = connection.getInputStream
        try{
          Files.copy(stream, incomplete, StandardCopyOption.REPLACE_EXISTING)
        } finally {
          stream.close()
        }
        sha1.foreach{
          hash =>
            val expected = hash
            val actual = this.sha1(Files.readAllBytes(incomplete))
            assert( expected == actual, s"$expected == $actual" )
            logger.resolver( green("verified") ++ " checksum for " ++ target.string)
        }
        Files.move(incomplete, Paths.get(target.string), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        true
      }
    }
  }

  def listFilesRecursive(f: File): Seq[File] = {
    f +: (
      if( f.isDirectory ) f.listFiles.flatMap(listFilesRecursive).toVector else Seq[File]()
    )
  }

  // ========== compilation / execution ==========

  def runMainIfFound(cls: String, args: Seq[String], classLoader: ClassLoader ): ExitCode = {
    if( classLoader.canLoad(cls) ){
      runMain(cls, args, classLoader )
    } else ExitCode.Success
  }

  def runMain(cls: String, args: Seq[String], classLoader: ClassLoader ): ExitCode = {
    logger.lib(s"Running $cls.main($args) with classLoader: " ++ classLoader.toString)
    trapExitCode{
      classLoader
        .loadClass(cls)
        .getMethod( "main", classOf[Array[String]] )
        .invoke( null, args.toArray.asInstanceOf[AnyRef] )
      ExitCode.Success
    }
  }

  implicit class ClassLoaderExtensions(classLoader: ClassLoader){
    def canLoad(className: String) = {
      try{
        classLoader.loadClass(className)
        true
      } catch {
        case e: ClassNotFoundException => false
      }
    }
  }

  def needsUpdate( sourceFiles: Seq[File], statusFile: File ) = {
    val lastCompile = statusFile.lastModified
    sourceFiles.filter(_.lastModified > lastCompile).nonEmpty
  }

  def compile(
    needsRecompile: Boolean,
    files: Seq[File],
    compileTarget: File,
    statusFile: File,
    classpath: ClassPath,
    scalacOptions: Seq[String] = Seq(),
    classLoaderCache: ClassLoaderCache,
    zincVersion: String,
    scalaVersion: String
  ): Option[File] = {

    val cp = classpath.string
    if(classpath.files.isEmpty)
      throw new Exception("Trying to compile with empty classpath. Source files: " ++ files.toString)

    if( files.isEmpty ){
      None
    }else{
      if( needsRecompile ){
        import MavenRepository.central
        val zinc = central.resolveOne(MavenDependency("com.typesafe.zinc","zinc", zincVersion))
        val zincDeps = zinc.transitiveDependencies
        
        val sbtInterface =
          zincDeps
            .collect{ case d @
              BoundMavenDependency(
                MavenDependency( "com.typesafe.sbt", "sbt-interface", _, Classifier.none),
                _
              ) => d
            }
            .headOption
            .getOrElse( throw new Exception(s"cannot find sbt-interface in zinc $zincVersion dependencies: "++zincDeps.toString) )
            .jar

        val compilerInterface =
          zincDeps
            .collect{ case d @
              BoundMavenDependency(
                MavenDependency( "com.typesafe.sbt", "compiler-interface", _, Classifier.sources),
                _
              ) => d
            }
            .headOption
            .getOrElse( throw new Exception(s"cannot find compiler-interface in zinc $zincVersion dependencies: "++zincDeps.toString) )
            .jar

        val scalaLibrary = central.resolveOne(MavenDependency("org.scala-lang","scala-library",scalaVersion)).jar
        val scalaReflect = central.resolveOne(MavenDependency("org.scala-lang","scala-reflect",scalaVersion)).jar
        val scalaCompiler = central.resolveOne(MavenDependency("org.scala-lang","scala-compiler",scalaVersion)).jar

        val start = System.currentTimeMillis

        val code = redirectOutToErr{
          lib.runMain(
            "com.typesafe.zinc.Main",
            Seq(
              "-scala-compiler", scalaCompiler.toString,
              "-scala-library", scalaLibrary.toString,
              "-sbt-interface", sbtInterface.toString,
              "-compiler-interface", compilerInterface.toString,
              "-scala-extra", scalaReflect.toString,
              "-cp", cp,
              "-d", compileTarget.toString
            ) ++ scalacOptions.map("-S"++_) ++ files.map(_.toString),
            zinc.classLoader(classLoaderCache)
          )
        }

        if(code == ExitCode.Success){
          // write version and when last compilation started so we can trigger
          // recompile if cbt version changed or newer source files are seen
          Files.write(statusFile.toPath, "".getBytes)//cbtVersion.getBytes)
          Files.setLastModifiedTime(statusFile.toPath, FileTime.fromMillis(start) )
        } else {
          System.exit(code.integer) // FIXME: let's find a better solution for error handling. Maybe a monad after all.
        }
      }
      Some( compileTarget )
    }
  }
  def redirectOutToErr[T](code: => T): T = {
    val oldOut = System.out
    try{
      System.setOut(System.err)
      code
    } finally{
      System.setOut(oldOut)
    }
  }

  def trapExitCode( code: => ExitCode ): ExitCode = {
    try{
      System.setSecurityManager( new TrapSecurityManager )
      code
    } catch {
      case CatchTrappedExitCode(exitCode) =>
        exitCode
    } finally {
      System.setSecurityManager(NailgunLauncher.defaultSecurityManager)
    }
  }

  def ScalaDependency(
    groupId: String, artifactId: String, version: String, classifier: Classifier = Classifier.none,
    scalaMajorVersion: String
  ) =
    MavenDependency(
      groupId, artifactId ++ "_" ++ scalaMajorVersion, version, classifier
    )
}
