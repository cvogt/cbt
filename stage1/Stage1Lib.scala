package cbt

import cbt.paths._

import java.io._
import java.lang.reflect.InvocationTargetException
import java.net._
import java.nio.file._
import javax.tools._
import java.security._
import java.util._
import javax.xml.bind.annotation.adapters.HexBinaryAdapter

import scala.collection.immutable.Seq

// CLI interop
case class ExitCode(code: Int)
object ExitCode{
  val Success = ExitCode(0)
  val Failure = ExitCode(1)
}

class TrappedExitCode(private val exitCode: Int) extends Exception
object TrappedExitCode{
  def unapply(e: Throwable): Option[ExitCode] =
    Option(e) flatMap {
      case i: InvocationTargetException => unapply(i.getTargetException)
      case e: TrappedExitCode => Some( ExitCode(e.exitCode) )
      case _ => None
    }
}

case class Context( cwd: File, args: Seq[String], logger: Logger )

class BaseLib{
  def realpath(name: File) = new File(Paths.get(name.getAbsolutePath).normalize.toString)
}

class Stage1Lib( val logger: Logger ) extends BaseLib{
  lib =>
  implicit val implicitLogger: Logger = logger

  def scalaMajorVersion(scalaMinorVersion: String) = scalaMinorVersion.split("\\.").take(2).mkString(".")

  // ========== reflection ==========

  /** Create instance of the given class via reflection */
  def create(cls: String)(args: Any*)(classLoader: ClassLoader): Any = {
    logger.composition( logger.showInvocation("Stage1Lib.create", (classLoader,cls,args)) )
    import scala.reflect.runtime.universe._
    val m = runtimeMirror(classLoader)
    val sym = m.classSymbol(classLoader.loadClass(cls))
    val cm = m.reflectClass( sym.asClass )
    val tpe = sym.toType
    val ctorm = cm.reflectConstructor( tpe.decl(termNames.CONSTRUCTOR).asMethod )
    ctorm(args:_*)
  }

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

  def download(urlString: URL, target: File, sha1: Option[String]){
    val incomplete = Paths.get( target.string ++ ".incomplete" );
    if( !target.exists ){
      target.getParentFile.mkdirs
      logger.resolver(blue("downloading ") ++ urlString.string)
      logger.resolver(blue("to ") ++ target.string)
      val stream = urlString.openStream
      Files.copy(stream, incomplete, StandardCopyOption.REPLACE_EXISTING)
      sha1.foreach{
        hash =>
          val expected = hash
          val actual = this.sha1(Files.readAllBytes(incomplete))
          assert( expected == actual, s"$expected == $actual" )
          logger.resolver( green("verified") ++ " checksum for " ++ target.string)
      }
      stream.close
      Files.move(incomplete, Paths.get(target.string), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
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
        .getMethod( "main", scala.reflect.classTag[Array[String]].runtimeClass )
        .invoke( null, args.toArray.asInstanceOf[AnyRef] )
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

  def zinc(
    needsRecompile: Boolean,
    files: Seq[File],
    compileTarget: File,
    classpath: ClassPath,
    extraArgs: Seq[String] = Seq()
  )( zincVersion: String, scalaVersion: String ): Unit = {

    val cp = classpath.string
    if(classpath.files.isEmpty)
      throw new Exception("Trying to compile with empty classpath. Source files: " ++ files.toString)

    if(files.isEmpty)
      throw new Exception("Trying to compile no files. ClassPath: " ++ cp)

    // only run zinc if files changed, for performance reasons
    // FIXME: this is broken, need invalidate on changes in dependencies as well
    if( needsRecompile ){
      val zinc = JavaDependency("com.typesafe.zinc","zinc", zincVersion)
      val zincDeps = zinc.transitiveDependencies
      
      val sbtInterface =
        zincDeps
          .collect{ case d @ JavaDependency( "com.typesafe.sbt", "sbt-interface", _, Classifier.none ) => d }
          .headOption
          .getOrElse( throw new Exception(s"cannot find sbt-interface in zinc $zincVersion dependencies: "++zincDeps.toString) )
          .jar

      val compilerInterface =
        zincDeps
          .collect{ case d @ JavaDependency( "com.typesafe.sbt", "compiler-interface", _, Classifier.sources ) => d }
          .headOption
          .getOrElse( throw new Exception(s"cannot find compiler-interface in zinc $zincVersion dependencies: "++zincDeps.toString) )
          .jar

      val scalaLibrary = JavaDependency("org.scala-lang","scala-library",scalaVersion).jar
      val scalaReflect = JavaDependency("org.scala-lang","scala-reflect",scalaVersion).jar
      val scalaCompiler = JavaDependency("org.scala-lang","scala-compiler",scalaVersion).jar

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
          ) ++ extraArgs.map("-S"++_) ++ files.map(_.toString),
          zinc.classLoader
        )
      }

      if(code != ExitCode.Success){
        // Ensure we trigger recompilation next time. This is currently required because we
        // don't record the time of the last successful build elsewhere. But hopefully that will
        // change soon.
        val now = System.currentTimeMillis()
        files.foreach(_.setLastModified(now))

        // Tell the caller that things went wrong.
        System.exit(code.code)
      }
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

  private val trapSecurityManager = new SecurityManager {
    override def checkPermission( permission: Permission ) = {
      /*
      NOTE: is it actually ok, to just make these empty?
      Calling .super leads to ClassNotFound exteption for a lambda.
      Calling to the previous SecurityManager leads to a stack overflow
      */
    }
    override def checkPermission( permission: Permission, context: Any ) = {
      /* Does this methods need to be overidden? */
    }
    override def checkExit( status: Int ) = {
      super.checkExit(status)
      logger.lib(s"checkExit($status)")
      throw new TrappedExitCode(status)
    }
  }

  def trapExitCode( code: => Unit ): ExitCode = {
    try{
      System.setSecurityManager( trapSecurityManager )
      code
      ExitCode.Success
    } catch {
      case TrappedExitCode(exitCode) =>
        exitCode
    } finally {
      System.setSecurityManager(NailgunLauncher.defaultSecurityManager)
    }
  }

  def ScalaDependency(
    groupId: String, artifactId: String, version: String, classifier: Classifier = Classifier.none,
    scalaVersion: String
  ) =
    JavaDependency(
      groupId, artifactId ++ "_" ++ scalaVersion, version, classifier
    )
}