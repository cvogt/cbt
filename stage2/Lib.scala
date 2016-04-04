package cbt
import cbt.paths._

import java.io._
import java.net._
import java.lang.reflect.InvocationTargetException
import java.nio.file.{Path =>_,_}
import java.nio.file.Files.readAllBytes
import java.security.MessageDigest
import java.util.jar._
import java.lang.reflect.Method

import scala.collection.immutable.Seq
import scala.util._

// pom model
case class Developer(id: String, name: String, timezone: String, url: URL)
case class License(name: String, url: URL)

/** Don't extend. Create your own libs :). */
final class Lib(logger: Logger) extends Stage1Lib(logger) with Scaffold{
  lib =>

  val buildClassName = "Build"
  val buildBuildClassName = "BuildBuild"

  /** Loads Build for given Context */
  def loadDynamic(context: Context, default: Context => Build = new Build(_)): Build = {
    context.logger.composition( context.logger.showInvocation("Build.loadDynamic",context) )
    loadRoot(context, default).finalBuild
  }
  /**
  Loads whatever Build needs to be executed first in order to eventually build the build for the given context.
  This can either the Build itself, of if exists a BuildBuild or a BuildBuild for a BuildBuild and so on.
  */
  def loadRoot(context: Context, default: Context => Build = new Build(_)): Build = {
    context.logger.composition( context.logger.showInvocation("Build.loadRoot",context) )
    def findStartDir(cwd: File): File = {
      val buildDir = realpath( cwd ++ "/build" )
      if(buildDir.exists) findStartDir(buildDir) else cwd
    }

    val start = findStartDir(context.cwd)

    val useBasicBuildBuild = context.cwd == start

    val rootBuildClassName = if( useBasicBuildBuild ) buildBuildClassName else buildClassName
    try{
      if(useBasicBuildBuild) default( context ) else new cbt.BuildBuild( context.copy( cwd = start ) )
    } catch {
      case e:ClassNotFoundException if e.getMessage == rootBuildClassName =>
        throw new Exception(s"no class $rootBuildClassName found in " ++ start.string)
    }
  }

  def srcJar(sourceFiles: Seq[File], artifactId: String, version: String, jarTarget: File): Option[File] = {
    lib.jarFile(
      jarTarget ++ ("/"++artifactId++"-"++version++"-sources.jar"),
      sourceFiles
    )
  }

  def jar(artifactId: String, version: String, compileTarget: File, jarTarget: File): Option[File] = {
    lib.jarFile(
      jarTarget ++ ("/"++artifactId++"-"++version++".jar"),
      Seq(compileTarget)
    )
  }

  def docJar(
    scalaVersion: String,
    sourceFiles: Seq[File],
    dependencyClasspath: ClassPath,
    apiTarget: File,
    jarTarget: File,
    artifactId: String,
    version: String,
    compileArgs: Seq[String],
    classLoaderCache: ClassLoaderCache
  ): Option[File] = {
    if(sourceFiles.isEmpty){
      None
    } else {
      apiTarget.mkdirs
      val args = Seq(
        // FIXME: can we use compiler dependency here?
        "-cp", dependencyClasspath.string, // FIXME: does this break for builds that don't have scalac dependencies?
        "-d",  apiTarget.toString
      ) ++ compileArgs ++ sourceFiles.map(_.toString)
      logger.lib("creating docs for source files "+args.mkString(", "))
      redirectOutToErr{
        runMain(
          "scala.tools.nsc.ScalaDoc",
          args,
          ScalaDependencies(scalaVersion)(logger).classLoader(classLoaderCache)
        )
      }
      lib.jarFile(
        jarTarget ++ ("/"++artifactId++"-"++version++"-javadoc.jar"),
        Vector(apiTarget)
      )
    }
  }

  def test( context: Context ): ExitCode = {
    val loggers = logger.enabledLoggers.mkString(",")
    // FIXME: this is a hack to pass logger args on to the tests.
    // should probably have a more structured way
    val loggerArg = if(loggers != "") Some("-Dlog="++loggers) else None

    logger.lib(s"invoke testDefault( $context )")
    val exitCode: ExitCode = loadDynamic(
      context.copy( cwd = context.cwd ++ "/test", args = loggerArg.toVector ++ context.args ),
      new Build(_) with mixins.Test
    ).run
    logger.lib(s"return testDefault( $context )")
    exitCode
  }

  // task reflection helpers
  def tasks(cls:Class[_]): Map[String, Method] =
    Stream
      .iterate(cls.asInstanceOf[Class[Any]])(_.getSuperclass)
      .takeWhile(_ != null)
      .toVector
      .dropRight(1) // drop Object
      .reverse
      .flatMap(
        c =>
          c
          .getDeclaredMethods
          .filterNot( _.getName contains "$" )
          .filter{ m =>
            java.lang.reflect.Modifier.isPublic(m.getModifiers)
          }
          .filter( _.getParameterTypes.length == 0 )
          .map(m => NameTransformer.decode(m.getName) -> m)
      ).toMap

  def taskNames(cls: Class[_]): Seq[String] = tasks(cls).keys.toVector.sorted

  def usage(buildClass: Class[_], context: Context): String = {
    val baseTasks = lib.taskNames(classOf[Build])
    val thisTasks = lib.taskNames(buildClass) diff baseTasks
    (
      (
        if( thisTasks.nonEmpty ){
          s"""Methods provided by Build ${context.cwd}

  ${thisTasks.mkString("  ")}

"""
        } else ""
      ) ++ s"""Methods provided by CBT (but possibly overwritten)

  ${baseTasks.mkString("  ")}"""
      ) ++ "\n"
  }

  class ReflectBuild[T:scala.reflect.ClassTag](build: Build) extends ReflectObject(build){
    def usage = lib.usage(build.getClass, build.context)
  }
  abstract class ReflectObject[T:scala.reflect.ClassTag](obj: T){
    def usage: String
    def callNullary( taskName: Option[String] ): Unit = {
      val ts = tasks(obj.getClass)
      taskName.map( NameTransformer.encode ).flatMap(ts.get).map{ method =>
        val result: Option[Any] = Option(method.invoke(obj)) // null in case of Unit
        result.flatMap{
          case v: Option[_] => v
          case other => Some(other)
        }.map{
          value =>
          // Try to render console representation. Probably not the best way to do this.
          scala.util.Try( value.getClass.getDeclaredMethod("toConsole") ) match {
            case scala.util.Success(toConsole) =>
              println(toConsole.invoke(value))

            case scala.util.Failure(e) if Option(e.getMessage).getOrElse("") contains "toConsole" =>
              value match {
                case ExitCode(code) => System.exit(code)
                case other => println( other.toString ) // no method .toConsole, using to String
              }

            case scala.util.Failure(e) =>
              throw e
          }
        }.getOrElse("")
      }.getOrElse{
        taskName.foreach{ n =>
          System.err.println(s"Method not found: $n")
          System.err.println("")
        }
        System.err.println(usage)
        taskName.foreach{ _ =>
          ExitCode.Failure
        }
      }
    }
  }

  // file system helpers
  def basename(path: File): String = path.toString.stripSuffix("/").split("/").last
  def dirname(path: File): File = new File(realpath(path).string.stripSuffix("/").split("/").dropRight(1).mkString("/"))
  def nameAndContents(file: File) = basename(file) -> readAllBytes(Paths.get(file.toString))

  def jarFile( jarFile: File, files: Seq[File] ): Option[File] = {
    if( files.isEmpty ){
      None
    } else {
      logger.lib("Start packaging "++jarFile.string)
      val manifest = new Manifest
      manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0")
      val jar = new JarOutputStream(new FileOutputStream(jarFile.toString), manifest)

      val names = for {
        base <- files.filter(_.exists).map(realpath)
        file <- listFilesRecursive(base) if file.isFile
      } yield {
          val name = if(base.isDirectory){
            file.toString stripPrefix base.toString
          } else file.toString
          val entry = new JarEntry( name )
          entry.setTime(file.lastModified)
          jar.putNextEntry(entry)
          jar.write( readAllBytes( Paths.get(file.toString) ) )
          jar.closeEntry
          name
      }

      val duplicateFiles = (names diff names.distinct).distinct
      assert(
        duplicateFiles.isEmpty,
        s"Conflicting file names when trying to create $jarFile: "++duplicateFiles.mkString(", ")
      )

      jar.close
      logger.lib("Done packaging " ++ jarFile.toString)

      Some(jarFile)
    }
  }

  lazy val passphrase =
    Option(System.console).getOrElse(
      throw new Exception("Can't access Console. This probably shouldn't be run through Nailgun.")
    ).readPassword(
      "GPG Passphrase please:"
    ).mkString

  def sign(file: File): File = {
    //http://stackoverflow.com/questions/16662408/correct-way-to-sign-and-verify-signature-using-bouncycastle
    val statusCode =
      new ProcessBuilder( "gpg", "--batch", "--yes", "-a", "-b", "-s", "--passphrase", passphrase, file.toString )
        .inheritIO.start.waitFor

    if( 0 != statusCode ) throw new Exception("gpg exited with status code " ++ statusCode.toString)

    file ++ ".asc"
  }

  //def requiredForPom[T](name: String): T = throw new Exception(s"You need to override `def $name` in order to generate a valid pom.")

  def pom(
    groupId: String,
    artifactId: String,
    version: String,
    name: String,
    description: String,
    url: URL,
    developers: Seq[Developer],
    licenses: Seq[License],
    scmUrl: String, // seems like invalid URLs are used here in pom files
    scmConnection: String,
    dependencies: Seq[Dependency],
    pomExtra: Seq[scala.xml.Node],
    jarTarget: File
  ): File = {
    val xml =
      <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0">
          <modelVersion>4.0.0</modelVersion>
          <groupId>{groupId}</groupId>
          <artifactId>{artifactId}</artifactId>
          <version>{version}</version>
          <packaging>jar</packaging>
          <name>{name}</name>
          <description>{description}</description>
          <url>{url}</url>
          <licenses>
            {licenses.map{ license =>
            <license>
              <name>{license.name}</name>
                <url>{license.url}</url>
              <distribution>repo</distribution>
            </license>
            }}
          </licenses>
          <developers>
            {developers.map{ developer =>
            <developer>
              <id>{developer.id}</id>
              <name>{developer.name}</name>
              <timezone>{developer.timezone}</timezone>
              <url>{developer.url}</url>
            </developer>
            }}
          </developers>
          <scm>
            <url>{scmUrl}</url>
            <connection>{scmConnection}</connection>
          </scm>
          {pomExtra}
          <dependencies>
          {
            dependencies.map{
              case d:ArtifactInfo =>
                <dependency>
                    <groupId>{d.groupId}</groupId>
                    <artifactId>{d.artifactId}</artifactId>
                    <version>{d.version}</version>
                </dependency>
            }
          }
          </dependencies>
      </project>
    val path = jarTarget.toString ++ ( "/" ++ artifactId ++ "-" ++ version ++ ".pom" )
    val file = new File(path)
    Files.write(file.toPath, ("<?xml version='1.0' encoding='UTF-8'?>\n" ++ xml.toString).getBytes)
    file
  }

  def concurrently[T,R]( concurrencyEnabled: Boolean )( items: Seq[T] )( projection: T => R ): Seq[R] = {
    if(concurrencyEnabled) items.par.map(projection).seq
    else items.map(projection)
  }

  def publishSnapshot( sourceFiles: Seq[File], artifacts: Seq[File], url: URL ): Unit = {
    if(sourceFiles.nonEmpty){
      val files = artifacts.map(nameAndContents)
      uploadAll(url, files)
    }
  }

  def publishSigned( sourceFiles: Seq[File], artifacts: Seq[File], url: URL ): Unit = {
    // TODO: make concurrency configurable here
    if(sourceFiles.nonEmpty){
      val files = (artifacts ++ artifacts.map(sign)).map(nameAndContents)
      lazy val checksums = files.flatMap{
        case (name, content) => Seq(
          name++".md5" -> md5(content).toArray.map(_.toByte),
          name++".sha1" -> sha1(content).toArray.map(_.toByte)
        )
      }
      val all = (files ++ checksums)
      uploadAll(url, all)
    }
  }


  def uploadAll(url: URL, nameAndContents: Seq[(String, Array[Byte])]): Unit =
    nameAndContents.map{ case(name, content) => upload(name, content, url) }

  def upload(fileName: String, fileContents: Array[Byte], baseUrl: URL): Unit = {
    import java.net._
    import java.io._
    logger.task("uploading "++fileName)
    val url = baseUrl ++ fileName
    val httpCon = url.openConnection.asInstanceOf[HttpURLConnection]
    httpCon.setDoOutput(true)
    httpCon.setRequestMethod("PUT")
    val userPassword = new String(readAllBytes(sonatypeLogin.toPath)).trim
    val encoding = new sun.misc.BASE64Encoder().encode(userPassword.getBytes)
    httpCon.setRequestProperty("Authorization", "Basic " ++ encoding)
    httpCon.setRequestProperty("Content-Type", "application/binary")
    httpCon.getOutputStream.write(
      fileContents
    )
    httpCon.getInputStream
  }


  // code for continuous compile
  def watch(files: Seq[File])(action: PartialFunction[File, Unit]): Unit = {
    import com.barbarysoftware.watchservice._
    import scala.collection.JavaConversions._
    val watcher = WatchService.newWatchService

    val realFiles = files.map(realpath)

    realFiles.map{
      // WatchService can only watch folders
      case file if file.isFile => dirname(file)
      case file => file
    }.distinct.map{ file =>
      val watchableFile = new WatchableFile(file)
      val key = watchableFile.register(
        watcher,
        StandardWatchEventKind.ENTRY_CREATE,
        StandardWatchEventKind.ENTRY_DELETE,
        StandardWatchEventKind.ENTRY_MODIFY
      )
    }

    scala.util.control.Breaks.breakable{
      while(true){
        logger.loop("Waiting for file changes...")
        logger.loop("Waiting for file changes...2")
        Option(watcher.take).map{
          key =>
          val changedFiles = key
            .pollEvents
            .toVector
            .filterNot(_.kind == StandardWatchEventKind.OVERFLOW)
            .map(_.context.toString)
            // make sure we don't react on other files changed
            // in the same folder like the files we care about
            .filter{ name => realFiles.exists(name startsWith _.toString) }
            .map(new File(_))

          changedFiles.foreach( f => logger.loop( "Changed: " ++ f.toString ) )
          changedFiles.collect(action)
          key.reset
        }
      }
    }
  }
}
