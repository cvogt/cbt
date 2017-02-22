package cbt

import java.io._
import java.net._
import java.lang.reflect.InvocationTargetException
import java.nio.file.{Path =>_,_}
import java.nio.file.Files.{readAllBytes, deleteIfExists, delete}
import java.security.MessageDigest
import java.util.jar._
import java.lang.reflect.Method

import scala.util._

// pom model
case class Developer(id: String, name: String, timezone: String, url: URL)

/** Don't extend. Create your own libs :). */
final class Lib(val logger: Logger) extends Stage1Lib(logger){
  lib =>

  val buildFileName = "build.scala"
  val buildDirectoryName = "build"
  val buildClassName = "Build"
  val buildBuildClassName = "BuildBuild"

  /**
  Loads whatever Build needs to be executed first in order to eventually build the build for the given context.
  This can either the Build itself, of if exists a BuildBuild or a BuildBuild for a BuildBuild and so on.
  */
  def loadRoot(context: Context): BuildInterface = {
    val directory = context.workingDirectory

    context.logger.composition( context.logger.showInvocation("Lib.loadRoot",directory) )

    val start = lib.findInnerMostModuleDirectory(directory)

    val useBasicBuild = directory == start && start.getName != buildDirectoryName

    try{
      if(useBasicBuild) {
        new BasicBuild( context.copy( workingDirectory = directory ) )
      } else if(
        // essentials depends on eval, which has a build that depends on scalatest
        // this means in these we can't depend on essentials
        // hopefully we find a better way that this pretty hacky exclusion rule
        directory == (context.cbtHome ++ "/plugins/essentials")
        || directory == (context.cbtHome ++ "/libraries/eval")
        || directory == (context.cbtHome ++ "/plugins/scalatest")
      )
        new cbt.ConcreteBuildBuildWithoutEssentials( context.copy( workingDirectory = start ) )
      else
        new cbt.ConcreteBuildBuild( context.copy( workingDirectory = start ) )
    } catch {
      case e:ClassNotFoundException if e.getMessage == buildClassName =>
        throw new Exception(s"no class ${buildClassName} found in " ++ start.string)
    }
  }

  def srcJar(
    sourceFiles: Seq[File], artifactId: String, scalaMajorVersion: String, version: String, jarTarget: File,
    filter: File => Boolean, stripBase: File
  ): Option[File] = {
    lib.jarFile(
      jarTarget ++ ("/"++artifactId++"_"++scalaMajorVersion++"-"++version++"-sources.jar"),
      sourceFiles, filter=filter, stripBase=Some( stripBase )
    )
  }

  def jar(artifactId: String, scalaMajorVersion: String, version: String, files: Seq[File], jarTarget: File): Option[File] = {
    lib.jarFile(
      jarTarget ++ ("/"++artifactId++"_"++scalaMajorVersion++"-"++version++".jar"),
      files
    )
  }

  def docJar(
    cbtLastModified: Long,
    scalaVersion: String,
    sourceFiles: Seq[File],
    dependencyClasspath: ClassPath,
    docTarget: File,
    jarTarget: File,
    artifactId: String,
    scalaMajorVersion: String,
    version: String,
    compileArgs: Seq[String],
    mavenCache: File
  )(implicit transientCache: java.util.Map[AnyRef,AnyRef], classLoaderCache: ClassLoaderCache): Option[File] = {
    if(sourceFiles.isEmpty){
      None
    } else {
      docTarget.mkdirs
      val args = Seq(
        // FIXME: can we use compiler dependency here?
        "-cp", dependencyClasspath.string, // FIXME: does this break for builds that don't have scalac dependencies?
        "-d",  docTarget.toString
      ) ++ compileArgs ++ sourceFiles.map(_.toString)
      logger.lib("creating docs for source files "+args.mkString(", "))
      redirectOutToErr{
        runMain(
          "scala.tools.nsc.ScalaDoc",
          args,
          new ScalaDependencies(cbtLastModified,mavenCache,scalaVersion).classLoader
        )
      }
      lib.jarFile(
        jarTarget ++ ("/"++artifactId++"_"++scalaMajorVersion++"-"++version++"-javadoc.jar"),
        Vector(docTarget)
      )
    }
  }

  // task reflection helpers
  def taskMethods(cls:Class[_]): Map[String, Method] =
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

  def taskNames(cls: Class[_]): Seq[String] = taskMethods(cls).keys.toVector.sorted

  def usage(buildClass: Class[_], show: String): String = {
    val baseTasks = Seq(
      classOf[BasicBuild],
      classOf[PackageJars],
      classOf[Publish]
    ).flatMap(lib.taskNames).distinct.sorted
    val thisTasks = lib.taskNames(buildClass) diff baseTasks
    (
      (
        if( thisTasks.nonEmpty ){
          s"""Methods provided by ${show}

  ${thisTasks.mkString("  ")}

"""
        } else ""
      ) ++ s"""Methods provided by CBT (but possibly overwritten)

  ${baseTasks.mkString("  ")}"""
      ) ++ "\n"
  }

  def callReflective[T <: AnyRef]( obj: T, code: Option[String] ): ExitCode = {
    callInternal( obj, code.toSeq.flatMap(_.split("\\.").map( NameTransformer.encode )), Nil ) match {
      case (obj, code, None) =>
        val s = render(obj)
        if(s.nonEmpty)
          System.out.println(s)
        code getOrElse ExitCode.Success
      case (obj, code, Some(msg)) =>
        if(msg.nonEmpty)
          System.err.println(msg)
        val s = render(obj)
        if(s.nonEmpty)
          System.err.println(s)
        code getOrElse ExitCode.Failure
    }
  }

  private def render[T]( obj: T ): String = {
    obj match {
      case Some(s) => render(s)
      case None => ""
      case d: Dependency => lib.usage(d.getClass, d.show())
      case c: ClassPath => c.string
      case t:ToolsStage2.type => "Available methods: " ++ lib.taskNames(t.getClass).mkString("  ")
      case _ => obj.toString
    }
  }

  private def callInternal[T <: AnyRef]( obj: T, members: Seq[String], previous: Seq[String] ): (Option[Object], Option[ExitCode], Option[String]) = {
    members.headOption.map{ taskName =>
      logger.lib("Calling task " ++ taskName.toString)
      taskMethods(obj.getClass).get(taskName).map{ method =>
        Option(method.invoke(obj) /* null in case of Unit */ ).map{ result => 
          result match {
            case code if code.getClass.getSimpleName == "ExitCode" =>
              // FIXME: ExitCode needs to be part of the compatibility interfaces
              (None, Some(ExitCode(Stage0Lib.get(code,"integer").asInstanceOf[Int])), None)
            case Seq(bs @ _*) if bs.forall(_.isInstanceOf[BaseBuild]) =>
              bs.map( b => callInternal(b.asInstanceOf[BaseBuild], members.tail, previous :+ taskName) ).head
            case _ => callInternal(result, members.tail, previous :+ taskName)
          }
        }.getOrElse( (None, None, None) )
      }.getOrElse{
        ( Some(obj), None, Some("\nMethod not found: " ++ (previous :+ taskName).mkString(".") ++ "\n") )
      }
    }.getOrElse{
      ( Some(obj), None, None )
    }
  }

  def consoleOrFail(msg: String) = {
    Option(System.console).getOrElse(
      throw new Exception(msg + ". System.console() == null. See https://github.com/cvogt/cbt/issues/236")
    )
  }

  def clean(targets: Seq[File], force: Boolean, dryRun: Boolean, list: Boolean, help: Boolean): ExitCode = {
    def depthFirstFileStream(file: File): Vector[File] = {
      (
        if (file.isDirectory) {
          file.listFiles.toVector.flatMap(depthFirstFileStream(_))
        } else Vector()
      ) :+ file
    }
    lazy val files = targets.filter(_.exists).flatMap( depthFirstFileStream )

    if( help ){
      System.err.println( s"""
  list      lists files to be delete
  force     does not ask for confirmation
  dry-run   does not actually delete files
""" )
      ExitCode.Success
    } else if (files.isEmpty){
      System.err.println( "Nothing to clean." )
      ExitCode.Success
    } else if( list ){
      files.map(_.string).foreach( println )
      ExitCode.Success
    } else {
      val performDelete = (
        force || {
          val console = consoleOrFail("Use `cbt direct clean` or `cbt clean help`")
          System.err.println("Files to be deleted:\n")
          files.foreach( System.err.println )
          System.err.println("")
          System.err.print("To delete the above files type 'delete': ")
          console.readLine() == "delete"
        }
      )

      if( !performDelete ) {
        System.err.println( "Ok, not cleaning." )
        ExitCode.Failure
      } else {
        // use same Vector[File] that was displayed earlier as a safety measure
        files.foreach{ file => 
          System.err.println( red("Deleting") ++ " " ++ file.string )
          if(!dryRun){
            delete( file.toPath )
          }
        }
        System.err.println( "Done." )
        ExitCode.Success
      }
    }
  }

  // file system helpers
  def basename(path: File): String = path.toString.stripSuffix("/").split("/").last
  def dirname(path: File): File = new File(realpath(path).string.stripSuffix("/").split("/").dropRight(1).mkString("/"))
  def nameAndContents(file: File) = basename(file) -> readAllBytes(file.toPath)

  def sourceFiles( sources: Seq[File], sourceFileFilter: File => Boolean ): Seq[File] = {
    for {
      base <- sources.filter(_.exists).map(lib.realpath)
      file <- lib.listFilesRecursive(base) if file.isFile && sourceFileFilter(file)
    } yield file
  }

  // FIXME: for some reason it includes full path in docs
  def jarFile(
    jarFile: File, files: Seq[File], mainClass: Option[String] = None,
    filter: File => Boolean = _ => true, stripBase: Option[File] = None
  ): Option[File] = {
    val stripBaseCanonical = stripBase
    Files.deleteIfExists(jarFile.toPath)
    if( files.isEmpty ){
      None
    } else {
      jarFile.getParentFile.mkdirs
      logger.lib("Start packaging "++jarFile.string)
      val manifest = new Manifest()
      manifest.getMainAttributes.put( Attributes.Name.MANIFEST_VERSION, "1.0" )
      manifest.getMainAttributes.putValue( "Created-By", "Chris' Build Tool" )
      mainClass foreach { className =>
        manifest.getMainAttributes.put(Attributes.Name.MAIN_CLASS, className)
      }
      val jar = new JarOutputStream(new FileOutputStream(jarFile), manifest)
      try{
        val names = for {
          base <- files.filter(_.exists).map(realpath)
          file <- listFilesRecursive(base) if file.isFile
        } yield {
            val strip = Some( base ).filter(_.isDirectory) ++ stripBaseCanonical
            val name = strip.foldLeft( file.getCanonicalPath )(
              (f, prefix) => f.stripPrefix( prefix.getCanonicalPath ++ File.separator )
            )
            val entry = new JarEntry( name )
            entry.setTime(file.lastModified)
            jar.putNextEntry(entry)
            jar.write( readAllBytes( file.toPath ) )
            jar.closeEntry()
            name
        }

        val duplicateFiles = (names diff names.distinct).distinct
        assert(
          duplicateFiles.isEmpty,
          s"Conflicting file names when trying to create $jarFile: "++duplicateFiles.mkString(", ")
        )
      } finally {
        jar.close()
      }

      logger.lib("Done packaging " ++ jarFile.toString)

      Some(jarFile)
    }
  }

  lazy val passphrase =
    consoleOrFail( "Use `cbt direct <task>`" ).readPassword( "GPG Passphrase please:" ).mkString

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
    scalaMajorVersion: String,
    name: String,
    description: String,
    url: URL,
    developers: Seq[Developer],
    licenses: Seq[License],
    scmUrl: String, // seems like invalid URLs are used here in pom files
    scmConnection: String,
    inceptionYear: Int,
    organization: Option[Organization],
    dependencies: Seq[Dependency],
    jarTarget: File
  ): File = {
    val xml =
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>{groupId}</groupId>
    <artifactId>{artifactId ++ "_" ++ scalaMajorVersion}</artifactId>
    <version>{version}</version>
    <packaging>jar</packaging>
    <name>{name}</name>
    <description>{description}</description>
    <url>{url}</url>
    <licenses>
      {licenses.map{ license =>
      <license>
        <name>{license.name}</name>
        {license.url.map(url => <url>url</url>).getOrElse( scala.xml.NodeSeq.Empty )}
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
    <inceptionYear>{inceptionYear}</inceptionYear>
    {organization.map{ org =>
      <organization>
        <name>{org.name}</name>
        {org.url.map( url => <url>url</url> ).getOrElse( scala.xml.NodeSeq.Empty )}
      </organization>
    }.getOrElse(scala.xml.NodeSeq.Empty)}
    <dependencies>
    {dependencies.map{
      case d:ArtifactInfo =>
      <dependency>
        <groupId>{d.groupId}</groupId>
        <artifactId>{d.artifactId}</artifactId>
        <version>{d.version}</version>
      </dependency>
    }}
    </dependencies>
</project>
    // FIXME: do not build this file name including scalaMajorVersion in multiple places
    val path = jarTarget.toString ++ ( "/" ++ artifactId++ "_" ++ scalaMajorVersion ++ "-" ++ version  ++ ".pom" )
    val file = new File(path)
    write(file, "<?xml version='1.0' encoding='UTF-8'?>\n" ++ xml.toString)
  }

  def concurrently[T,R]( concurrencyEnabled: Boolean )( items: Seq[T] )( projection: T => R ): Seq[R] = {
    if(concurrencyEnabled) items.par.map(projection).seq
    else items.map(projection)
  }

  def publishUnsigned( sourceFiles: Seq[File], artifacts: Seq[File], url: URL, credentials: Option[String] = None ): Unit = {
    if(sourceFiles.nonEmpty){
      publish( artifacts, url, credentials )
    }
  }

  def publishLocal( sourceFiles: Seq[File], artifacts: Seq[File], mavenCache: File, releaseFolder: String ): Unit = {
    if(sourceFiles.nonEmpty){
      val targetDir = mavenCache ++ releaseFolder.stripSuffix("/")
      targetDir.mkdirs
      artifacts.foreach{ a =>
        val target = targetDir ++ ("/" ++ a.getName)
        System.err.println(blue("publishing ") ++ target.getPath)
        Files.copy( a.toPath, target.toPath, StandardCopyOption.REPLACE_EXISTING )
      }
    }
  }

  def publishSigned( artifacts: Seq[File], url: URL, credentials: Option[String] = None ): Unit = {
    // TODO: make concurrency configurable here
    publish( artifacts ++ artifacts.map(sign), url, credentials )
  }

  private def publish(artifacts: Seq[File], url: URL, credentials: Option[String]): Unit = {
    val files = artifacts.map(nameAndContents)
    lazy val checksums = files.flatMap{
      case (name, content) => Seq(
        name++".md5" -> md5(content).toArray.map(_.toByte),
        name++".sha1" -> sha1(content).toArray.map(_.toByte)
      )
    }
    val all = (files ++ checksums)
    uploadAll(url, all, credentials)
  }

  def uploadAll(url: URL, nameAndContents: Seq[(String, Array[Byte])], credentials: Option[String] = None ): Unit =
    nameAndContents.foreach { case (name, content) => upload(name, content, url, credentials ) }

  def upload(fileName: String, fileContents: Array[Byte], baseUrl: URL, credentials: Option[String] = None): Unit = {
    import java.net._
    import java.io._
    val url = baseUrl ++ "/" ++ fileName
    System.err.println(blue("uploading ") ++ url.toString)
    val httpCon = Stage0Lib.openConnectionConsideringProxy(url)
    httpCon.setDoOutput(true)
    httpCon.setRequestMethod("PUT")
    credentials.foreach(
      c => {
        val encoding = new sun.misc.BASE64Encoder().encode(c.getBytes)
        httpCon.setRequestProperty("Authorization", "Basic " ++ encoding)
      }
    )
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

  def findInnerMostModuleDirectory(directory: File): File = {
    val buildDir = realpath( directory ++ ("/" ++ lib.buildDirectoryName) )
    // do not appent buildFileName here, so that we detect empty build folders
    if(buildDir.exists) findInnerMostModuleDirectory(buildDir) else directory
  }
  def findOuterMostModuleDirectory(directory: File): File = {
    if(
      ( directory.getParentFile ++ ("/" ++ lib.buildDirectoryName) ).exists
    ) findOuterMostModuleDirectory(directory.getParentFile) else directory
  }
}
