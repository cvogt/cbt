package cbt

import java.io._
import java.lang.reflect.InvocationTargetException
import java.net._
import java.nio.charset.StandardCharsets
import java.nio.file._
import java.nio.file.attribute.FileTime
import javax.tools._
import java.security._
import java.util.{Set=>_,Map=>_,List=>_,Iterator=>_,_}
import javax.xml.bind.annotation.adapters.HexBinaryAdapter

class Stage1Lib( logger: Logger ) extends
  _root_.cbt.common_1.Module with
  _root_.cbt.reflect.Module with
  _root_.cbt.file.Module
{
  lib =>
  implicit protected val implicitLogger: Logger = logger

  def libMajorVersion(libFullVersion: String) = libFullVersion.split("\\.").take(2).mkString(".")

  // ========== file system / net ==========

  def array2hex(padTo: Int, array: Array[Byte]): String = {
    val hex = new java.math.BigInteger(1, array).toString(16)
    ("0" * (padTo-hex.size)) ++ hex
  }
  def md5( bytes: Array[Byte] ): String = array2hex(32, MessageDigest.getInstance("MD5").digest(bytes)).toLowerCase
  def sha1( bytes: Array[Byte] ): String = array2hex(40, MessageDigest.getInstance("SHA-1").digest(bytes)).toLowerCase

  def red(string: String) = scala.Console.RED++string++scala.Console.RESET
  def blue(string: String) = scala.Console.BLUE++string++scala.Console.RESET
  def green(string: String) = scala.Console.GREEN++string++scala.Console.RESET

  def write(file: File, content: String, options: OpenOption*): File = Stage0Lib.write(file, content, options:_*)
  def writeIfChanged(file: File, content: String, options: OpenOption*): File =
    if( !file.exists || content != file.readAsString ) write(file, content, options:_*) else file

  def addHttpCredentials( connection: HttpURLConnection, credentials: String ): Unit = {
    val encoding = new sun.misc.BASE64Encoder().encode(credentials.getBytes)
    connection.setRequestProperty("Authorization", "Basic " ++ encoding)
  }

  def download(url: URL, target: File, sha1: Option[String], replace: Boolean = false): Boolean = {
    if( target.exists && !replace ){
      logger.resolver(green("found ") ++ url.show)
      true
    } else {
      val incomplete = ( target ++ ".incomplete" ).toPath;
      val connection = Stage0Lib.openConnectionConsideringProxy(url)
      Option(url.getUserInfo).filter(_ != "").foreach(
        addHttpCredentials(connection,_)
      )
      if(connection.getResponseCode != HttpURLConnection.HTTP_OK){
        logger.resolver(blue("not found: ") ++ url.show)
        false
      } else {
        System.err.println(blue("downloading ") ++ url.show)
        logger.resolver(blue("to ") ++ target.string)
        target.getParentFile.mkdirs
        val stream = connection.getInputStream
        try{
          Files.copy(stream, incomplete, StandardCopyOption.REPLACE_EXISTING)
        } finally {
          stream.close
        }
        sha1.foreach{
          hash =>
            val expected = hash.toLowerCase
            val actual = this.sha1(Files.readAllBytes(incomplete))
            assert( expected == actual, s"$expected == $actual" )
            logger.resolver( green("verified") ++ " checksum for " ++ target.string)
        }
        Files.move(incomplete, target.toPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        true
      }
    }
  }

  def getCbtMain( cls: Class[_] ): cbt.reflect.StaticMethod[Context, ExitCode] =
    findStaticMethodOrFail[Context, ExitCode]( cls, "cbtMain" )

  def findCbtMain( cls: Class[_] ): Option[cbt.reflect.StaticMethod[Context, ExitCode]] =
    findStaticMethod[Context, ExitCode]( cls, "cbtMain" )

  /** shows an interactive dialogue in the shell asking the user to pick one of many choices */
  def pickOne[T]( msg: String, choices: Seq[T] )( show: T => String ): Option[T] = {
    if(choices.size == 0) None else if(choices.size == 1) Some(choices.head) else {
      Option(System.console).map{
        console =>
        val indexedChoices: Map[Int, T] = choices.zipWithIndex.toMap.mapValues(_+1).map(_.swap)
        System.err.println(
          indexedChoices.map{ case (index,choice) => s"[${index}] "++show(choice)}.mkString("\n")
        )
        val range = s"1 - ${indexedChoices.size}"
        System.err.println()
        System.err.println( msg ++ " [" ++ range ++ "] " )
        val answer = console.readLine()
        val choice = try{
          Some(Integer.parseInt(answer))
        }catch{
          case e:java.lang.NumberFormatException => None
        }

        choice.flatMap(indexedChoices.get).orElse{
          System.err.println("Not in range "++range)
          None
        }
      }.getOrElse{
        System.err.println("System.console() == null. Use `cbt direct <task>` or see https://github.com/cvogt/cbt/issues/236")
        None
      }
    }
  }

  /** interactively pick one main class */
  def pickClass( mainClasses: Seq[Class[_]] ): Option[Class[_]] = {
    pickOne( "Which one do you want to run?", mainClasses )( _.toString )
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

  def compile(
    cbtLastModified: Long,
    sourceFiles: Seq[File],
    compileTarget: File,
    statusFile: File,
    dependencies: Seq[Dependency],
    mavenCache: File,
    scalacOptions: Seq[String] = Seq(),
    zincVersion: String,
    scalaVersion: String
  )(
    implicit transientCache: java.util.Map[AnyRef, AnyRef], classLoaderCache: ClassLoaderCache
  ): Option[Long] = {
    val d = Dependencies(dependencies)
    val classpath = d.classpath
    val cp = classpath.string

    def lastModified = (
      cbtLastModified +: d.lastModified +: sourceFiles.map(_.lastModified)
    ).max

    if( sourceFiles.isEmpty ){
      None
    }else{
      val start = System.currentTimeMillis
      val lastCompiled = statusFile.lastModified
      if( lastModified > lastCompiled ){
        def Resolver(urls: URL*) = MavenResolver(cbtLastModified, mavenCache, urls: _*)
        val zinc = Resolver(mavenCentral).bindOne(MavenDependency("com.typesafe.zinc","zinc", zincVersion))
        val zincDeps = zinc.transitiveDependencies

        val sbtInterface =
          zincDeps
            .collect{ case d @
              BoundMavenDependency(
                _, _, MavenDependency( "com.typesafe.sbt", "sbt-interface", _, Classifier.none, _), _, _
              ) => d
            }
            .headOption
            .getOrElse( throw new Exception(s"cannot find sbt-interface in zinc $zincVersion dependencies: "++zincDeps.toString) )
            .jar

        val compilerInterface =
          zincDeps
            .collect{ case d @
              BoundMavenDependency(
                _, _, MavenDependency( "com.typesafe.sbt", "compiler-interface", _, Classifier.sources, _), _, _
              ) => d
            }
            .headOption
            .getOrElse( throw new Exception(s"cannot find compiler-interface in zinc $zincVersion dependencies: "++zincDeps.toString) )
            .jar

        val scalaLibrary = Resolver(mavenCentral).bindOne(MavenDependency("org.scala-lang","scala-library",scalaVersion)).jar
        val scalaReflect = Resolver(mavenCentral).bindOne(MavenDependency("org.scala-lang","scala-reflect",scalaVersion)).jar
        val scalaCompiler = Resolver(mavenCentral).bindOne(MavenDependency("org.scala-lang","scala-compiler",scalaVersion)).jar

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
          redirectOutToErr{
            System.err.println("Compiling to " ++ compileTarget.toString)
            try{
              zinc.runMain(
                _class,
                dualArgs ++ singleArgs ++ (
                  if(cp.isEmpty) Nil else Seq("-cp", cp)
                ) ++ sourceFiles.map(_.string)
              )
            } catch {
              case scala.util.control.NonFatal(e) =>
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
${if(cp.isEmpty) "" else ("  -classpath \\\n" ++ classpath.strings.mkString(":\\\n"))} \\
\\
${sourceFiles.sorted.mkString(" \\\n")}
"""
              )

              redirectOutToErr( e.printStackTrace )
              ExitCode.Failure
            }
          }

        if(code == ExitCode.Success){
          // write version and when last compilation started so we can trigger
          // recompile if cbt version changed or newer source files are seen
          write(statusFile, "")//cbtVersion.getBytes)
          Files.setLastModifiedTime(statusFile.toPath, FileTime.fromMillis(start) )
        } else {
          System.exit(code.integer) // FIXME: let's find a better solution for error handling. Maybe a monad after all.
        }
        Some( start )
      } else {
        Some( lastCompiled )
      }
    }
  }

  def getOutErrIn: (ThreadLocal[PrintStream], ThreadLocal[PrintStream], InputStream) =
    try{
      // trying nailgun's System.our/err wrapper
      val field = System.out.getClass.getDeclaredField("streams")
      val field2 = System.in.getClass.getDeclaredField("streams")
      assert(System.out.getClass.getName == "com.martiansoftware.nailgun.ThreadLocalPrintStream", System.out.getClass.getName)
      assert(System.err.getClass.getName == "com.martiansoftware.nailgun.ThreadLocalPrintStream", System.err.getClass.getName)
      assert(System.in.getClass.getName == "com.martiansoftware.nailgun.ThreadLocalInputStream", System.in.getClass.getName)
      field.setAccessible(true)
      field2.setAccessible(true)
      val out = field.get(System.out).asInstanceOf[ThreadLocal[PrintStream]]
      val err = field.get(System.err).asInstanceOf[ThreadLocal[PrintStream]]
      val in = field2.get(System.in).asInstanceOf[ThreadLocal[InputStream]]
      ( out, err, in.get )
    } catch {
      case e: NoSuchFieldException =>
        // trying cbt's System.our/err wrapper
        val field = classOf[FilterOutputStream].getDeclaredField("out")
        field.setAccessible(true)
        val outStream = field.get(System.out)
        val errStream = field.get(System.err)
        assert(outStream.getClass.getName == "cbt.ThreadLocalOutputStream", outStream.getClass.getName)
        assert(errStream.getClass.getName == "cbt.ThreadLocalOutputStream", errStream.getClass.getName)
        val field2 = outStream.getClass.getDeclaredField("threadLocal")
        field2.setAccessible(true)
        val out = field2.get(outStream).asInstanceOf[ThreadLocal[PrintStream]]
        val err = field2.get(errStream).asInstanceOf[ThreadLocal[PrintStream]]
        ( out, err, System.in )
    }

  def redirectOutToErr[T](code: => T): T = {
    val ( out, err, _ ) = getOutErrIn
    val oldOut: PrintStream = out.get
    out.set( err.get: PrintStream )
    val res = code
    out.set( oldOut )
    res
  }


  def ScalaDependency(
    groupId: String, artifactId: String, version: String, classifier: Classifier = Classifier.none,
    scalaMajorVersion: String, verifyHash: Boolean = true
  ) =
    MavenDependency(
      groupId, artifactId ++ "_" ++ scalaMajorVersion, version, classifier, verifyHash
    )

  def cacheOnDisk[T,J <: AnyRef : scala.reflect.ClassTag]
    ( cbtLastModified: Long, cacheFile: File, persistentCache: java.util.Map[AnyRef,AnyRef] )
    ( deserialize: String => T )
    ( serialize: T => String )
    ( dejavafy: J => Seq[T] )
    ( javafy: Seq[T] => J )
    ( compute: => Seq[T] ): Seq[T] = {
      val key = "cacheOnDisk:" + cacheFile
      Option( persistentCache.get(key) ).map(
        _.asInstanceOf[Array[AnyRef]]
      ).map{
        case Array(time: java.lang.Long, javafied: J) => (time, javafied)
      }.filter( _._1 > cbtLastModified )
       .map( _._2 )
       .map( dejavafy )
       .orElse{
        (cacheFile.exists && cacheFile.lastModified > cbtLastModified).option{
          import collection.JavaConverters._
          val v = Files
            .readAllLines( cacheFile.toPath, StandardCharsets.UTF_8 )
            .asScala
            .toStream
            .map( deserialize )
          persistentCache.put(key, Array(System.currentTimeMillis:java.lang.Long, javafy(v)))
          v
        }
      }.getOrElse{
        val result = compute
        val strings = result.map(serialize)
        val string = strings.mkString("\n")
        write(cacheFile, string)
        persistentCache.put(key, Array(System.currentTimeMillis:java.lang.Long, javafy(result)))
        result
      }
  }

  def dependencyTreeRecursion(root: Dependency, indent: Int = 0): String = (
    ( " " * indent )
    ++ root.show // (if(root.needsUpdate) red(root.show) else root.show)
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
    case d: ArtifactInfo =>
      val key = (d.groupId,d.artifactId)
      latest.get(key).getOrElse(
        throw new Exception( s"This should never happend. Could not find $key in \n"++latest.map{case (k,v) => k+" -> "+v}.mkString("\n") )
      )
    case d => d
  }

  def classLoaderRecursion( dependency: Dependency, latest: Map[(String,String),Dependency] )(implicit transientCache: java.util.Map[AnyRef,AnyRef], cache: ClassLoaderCache): ClassLoader = {
    // FIXME: shouldn't we be using KeyLockedLazyCache instead of hashmap directly here?
    val dependencies = dependency.dependencies.toVector
    val dependencyClassLoader: ClassLoader = {
      if( dependency.dependencies.isEmpty ){
        NailgunLauncher.jdkClassLoader
      } else if( dependencies.size == 1 ){
        classLoaderRecursion( dependencies.head, latest )
      } else{
        val lastModified = dependencies.map( _.lastModified ).max
        val cp = dependency.dependencyClasspath.string
        val cl =
          new MultiClassLoader(
            dependencies.map( classLoaderRecursion(_, latest) )
          )
        if( !cache.containsKey( cp, lastModified ) ){
          cache.put( cp, cl, lastModified )
        }
        cache.get( cp, lastModified )
      }
    }

    val a = actual( dependency, latest )
    def cl = new cbt.URLClassLoader( a.exportedClasspath, dependencyClassLoader )

    val cp = a.classpath.string
    val lastModified = a.lastModified
    if( !cache.containsKey( cp, lastModified ) ){
      cache.put( cp, cl, lastModified )
    }
    cache.get( cp, lastModified )
  }

  def addLoopFiles(cwd: File, files: Set[File]) = {
    lib.write(
      cwd / "target/.cbt-loop.tmp",
      files.map(_ + "\n").mkString,
      StandardOpenOption.APPEND
    )
  }
  def cached[T]( targetDirectory: File, inputLastModified: Long )( action: () => T ): (Option[T],Long) = {
    val t = targetDirectory
    val start = System.currentTimeMillis
    def lastSucceeded = t.lastModified
    def outputLastModified = t.listRecursive.diff(t :: Nil).map(_.lastModified).maxOption.getOrElse(0l)
    def updateSucceeded(time: Long) = Files.setLastModifiedTime( t.toPath, FileTime.fromMillis(time) )
    (
      ( inputLastModified >= lastSucceeded ).option{
        val result: T = action()
        updateSucceeded( start )
        result
      },
      outputLastModified
    )
  }

  def asyncPipeCharacterStreamSyncLines( inputStream: InputStream, outputStream: OutputStream, lock: AnyRef ): Thread = {
    new Thread(
      new Runnable{
        def run = {
          val b = new BufferedInputStream( inputStream )
          Iterator.continually{
            b.read // block until and read next character
          }.takeWhile(_ != -1).map{ c =>
            lock.synchronized{ // synchronize with other invocations
              outputStream.write(c)
              Iterator
                .continually( b.read )
                .takeWhile( _ != -1 )
                .map{ c =>
                  try{
                    outputStream.write(c)
                    outputStream.flush
                    (
                      c != '\n' // release lock when new line was encountered, allowing other writers to slip in
                      && b.available > 0 // also release when nothing is available to not block other outputs
                    )
                  } catch {
                    case e: IOException if e.getMessage == "Stream closed" => false
                  }
                }
                .takeWhile(identity)
                .length // force entire iterator
            }
          }.length // force entire iterator
        }
      }
    )
  }

  def asyncPipeCharacterStream( inputStream: InputStream, outputStream: OutputStream, continue: => Boolean ) = {
    new Thread(
      new Runnable{
        def run = {
          Iterator
            .continually{ inputStream.read }
            .takeWhile(_ != -1)
            .map{ c =>
              try{
                outputStream.write(c)
                outputStream.flush
                true
              } catch {
                case e: IOException if e.getMessage == "Stream closed" => false
              }
            }
            .takeWhile( identity )
            .takeWhile( _ => continue )
            .length // force entire iterator
        }
      }
    )
  }

  def runWithIO( commandLine: Seq[String], directory: Option[File] = None ): ExitCode = {
    val (out,err,in) = lib.getOutErrIn match { case (l,r, in) => (l.get,r.get, in) }
    val pb = new ProcessBuilder( commandLine: _* )
    val exitCode =
      if( !NailgunLauncher.runningViaNailgun ){
        pb.inheritIO.start.waitFor
      } else {
        val process = directory.map( pb.directory( _ ) ).getOrElse( pb )
            .redirectInput(ProcessBuilder.Redirect.PIPE)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start

        val lock = new AnyRef

        val t1 = lib.asyncPipeCharacterStreamSyncLines( process.getErrorStream, err, lock )
        val t2 = lib.asyncPipeCharacterStreamSyncLines( process.getInputStream, out, lock )
        val t3 = lib.asyncPipeCharacterStream( System.in, process.getOutputStream, process.isAlive )

        t1.start
        t2.start
        t3.start

        t1.join
        t2.join

        val e = process.waitFor
        System.err.println( scala.Console.RESET + "Please press ENTER to continue..." )
        t3.join
        e
      }

    ExitCode( exitCode )
  }
}

import scala.reflect._
import scala.language.existentials
case class PerClassCache(cache: java.util.Map[AnyRef,AnyRef], moduleKey: String)(implicit logger: Logger){
  def apply[D <: Dependency: ClassTag](key: AnyRef): MethodCache[D] = new MethodCache[D](key)
  case class MethodCache[D <: Dependency: ClassTag](key: AnyRef){
    def memoize[T <: AnyRef](task: => T): T = {
      val fullKey = (classTag[D].runtimeClass, moduleKey, key)
      logger.transientCache("fetching key"+fullKey)
      if( cache.containsKey(fullKey) ){
        logger.transientCache("found    key"+fullKey)
        cache.get(fullKey).asInstanceOf[T]
      } else{
        val value = task
        logger.transientCache("put      key"+fullKey)
        cache.put( fullKey, value )
        value
      }
    }
  }
}
