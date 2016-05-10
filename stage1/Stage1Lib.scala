package cbt

import java.io._
import java.lang.reflect.InvocationTargetException
import java.net._
import java.nio.charset.StandardCharsets
import java.nio.file._
import java.nio.file.attribute.FileTime
import javax.tools._
import java.security._
import java.util.{Set=>_,Map=>_,_}
import java.util.concurrent.ConcurrentHashMap
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
      case e: TrappedExitCode => Some( ExitCode(e.exitCode) )
      case _ => None
    }
  }
}

class BaseLib{
  def realpath(name: File) = new File(java.nio.file.Paths.get(name.getAbsolutePath).normalize.toString)
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
      logger.resolver(green("found ") ++ url.string)
      true
    } else {
      val incomplete = ( target ++ ".incomplete" ).toPath;
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
        Files.move(incomplete, target.toPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
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
    cbtHasChanged: Boolean,
    needsRecompile: Boolean,
    files: Seq[File],
    compileTarget: File,
    statusFile: File,
    classpath: ClassPath,
    mavenCache: File,
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
        def Resolver(urls: URL*) = MavenResolver(cbtHasChanged, mavenCache, urls: _*)
        val zinc = Resolver(mavenCentral).bindOne(MavenDependency("com.typesafe.zinc","zinc", zincVersion))
        val zincDeps = zinc.transitiveDependencies
        
        val sbtInterface =
          zincDeps
            .collect{ case d @
              BoundMavenDependency(
                _, _, MavenDependency( "com.typesafe.sbt", "sbt-interface", _, Classifier.none), _
              ) => d
            }
            .headOption
            .getOrElse( throw new Exception(s"cannot find sbt-interface in zinc $zincVersion dependencies: "++zincDeps.toString) )
            .jar

        val compilerInterface =
          zincDeps
            .collect{ case d @
              BoundMavenDependency(
                _, _, MavenDependency( "com.typesafe.sbt", "compiler-interface", _, Classifier.sources), _
              ) => d
            }
            .headOption
            .getOrElse( throw new Exception(s"cannot find compiler-interface in zinc $zincVersion dependencies: "++zincDeps.toString) )
            .jar

        val scalaLibrary = Resolver(mavenCentral).bindOne(MavenDependency("org.scala-lang","scala-library",scalaVersion)).jar
        val scalaReflect = Resolver(mavenCentral).bindOne(MavenDependency("org.scala-lang","scala-reflect",scalaVersion)).jar
        val scalaCompiler = Resolver(mavenCentral).bindOne(MavenDependency("org.scala-lang","scala-compiler",scalaVersion)).jar

        val start = System.currentTimeMillis

        val _class = "com.typesafe.zinc.Main"
        val dualArgs =
          Seq(
            "-scala-compiler", scalaCompiler.toString,
            "-scala-library", scalaLibrary.toString,
            "-sbt-interface", sbtInterface.toString,
            "-compiler-interface", compilerInterface.toString,
            "-scala-extra", scalaReflect.toString,
            "-d", compileTarget.toString
          )
        val singleArgs = scalacOptions.map( "-S" ++ _ )

        val code = 
          try{
            System.err.println("Compiling to " ++ compileTarget.toString)
            redirectOutToErr{
              lib.runMain(
                _class,
                dualArgs ++ singleArgs ++ Seq(
                  "-cp", cp // let's put cp last. It so long
                ) ++ files.map(_.toString),
                zinc.classLoader(classLoaderCache)
              )
            }
          } catch {
            case e: Exception =>
            System.err.println(red("The Scala compiler crashed. Try running it by hand:"))
            System.out.println(s"""
java -cp \\
${zinc.classpath.strings.mkString(":\\\n")} \\
\\
${_class} \\
\\
${dualArgs.grouped(2).map(_.mkString(" ")).mkString(" \\\n")} \\
\\
${singleArgs.mkString(" \\\n")} \\
\\
-cp \\
${classpath.strings.mkString(":\\\n")} \\
\\
${files.sorted.mkString(" \\\n")}
"""
            )
            ExitCode.Failure
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

  def cacheOnDisk[T]
    ( cbtHasChanged: Boolean, cacheFile: File )
    ( deserialize: String => T )
    ( serialize: T => String )
    ( compute: => Seq[T] ) = {
    if(!cbtHasChanged && cacheFile.exists){
      import collection.JavaConversions._
      Files
        .readAllLines( cacheFile.toPath, StandardCharsets.UTF_8 )
        .toStream
        .map(deserialize)
    } else {
      val result = compute
      val string = result.map(serialize).mkString("\n")
      Files.write(cacheFile.toPath, string.getBytes)
      result
    }
  }

  def dependencyTreeRecursion(root: Dependency, indent: Int = 0): String = (
    ( " " * indent )
    ++ (if(root.needsUpdate) red(root.show) else root.show)
    ++ root.dependencies.map( d =>
      "\n" ++ dependencyTreeRecursion(d,indent + 1)
    ).mkString
  )

  def transitiveDependencies(dependency: Dependency): Seq[Dependency] = {
    def linearize(deps: Seq[Dependency]): Seq[Dependency] = {
      // Order is important here in order to generate the correct lineraized dependency order for EarlyDependencies
      // (and maybe this as well in case we want to get rid of MultiClassLoader)
      try{
        if(deps.isEmpty) deps else ( deps ++ linearize(deps.flatMap(_.dependencies)) )
      } catch{
        case e: Exception => throw new Exception(dependency.show, e)
      }
    }

    // FIXME: this is probably wrong too eager.
    // We should consider replacing versions during traversals already
    // not just replace after traversals, because that could mean we
    // pulled down dependencies current versions don't even rely
    // on anymore.

    val deps: Seq[Dependency] = linearize(dependency.dependencies).reverse.distinct.reverse
    val hasInfo: Seq[Dependency with ArtifactInfo] = deps.collect{ case d:Dependency with ArtifactInfo => d }
    val noInfo: Seq[Dependency]  = deps.filter{
      case _:Dependency with ArtifactInfo => false
      case _ => true
    }
    noInfo ++ BoundMavenDependency.updateOutdated( hasInfo ).reverse.distinct
  }


  def actual(current: Dependency, latest: Map[(String,String),Dependency]) = current match {
    case d: ArtifactInfo => latest((d.groupId,d.artifactId))
    case d => d
  }

  def classLoaderRecursion( dependency: Dependency, latest: Map[(String,String),Dependency], cache: ClassLoaderCache ): ClassLoader = {
    val d = dependency
    val dependencies = dependency.dependencies
    def dependencyClassLoader( latest: Map[(String,String),Dependency], cache: ClassLoaderCache ): ClassLoader = {
      if( dependency.dependencies.isEmpty ){
        // wrap for caching
        new cbt.URLClassLoader( ClassPath(Seq()), ClassLoader.getSystemClassLoader().getParent() )
      } else if( dependencies.size == 1 ){
        classLoaderRecursion( dependencies.head, latest, cache )
      } else{
        val cp = d.dependencyClasspath.string
        if( dependencies.exists(_.needsUpdate) && cache.persistent.containsKey(cp) ){
          cache.persistent.remove(cp)
        }
        def cl = new MultiClassLoader( dependencies.map( classLoaderRecursion(_, latest, cache) ) )
        if(d.isInstanceOf[BuildInterface])
          cl // Don't cache builds right now. We need to fix invalidation first.
        else
          cache.persistent.get( cp, cl )
      }
    }

    val a = actual( dependency, latest )
    def cl = new cbt.URLClassLoader( a.exportedClasspath, dependencyClassLoader(latest, cache) )
    if(d.isInstanceOf[BuildInterface])
      cl
    else
      cache.persistent.get( a.classpath.string, cl )
  }
}
