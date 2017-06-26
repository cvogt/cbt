package cbt

import java.io._
import java.net._
import java.lang.reflect.InvocationTargetException
import java.nio.file.{Path => _, _}
import java.nio.file.Files._
import java.security.MessageDigest
import java.util.jar._
import java.lang.reflect.Method

import scala.util._
import scala.reflect.{ClassTag, NameTransformer}
import scala.reflect.runtime.universe

// pom model
case class Developer(id: String, name: String, timezone: String, url: URL)

/** Don't extend. Create your own libs :). */
final class Lib(val logger: Logger) extends
  Stage1Lib(logger) with
  _root_.cbt.process.Module
{
  lib =>

  val buildFileName = "build.scala"
  val buildDirectoryName = "build"
  val buildClassName = "Build"
  val buildBuildClassName = "BuildBuild"

  def scaladoc(
    cbtLastModified: Long,
    scalaVersion: String,
    sourceFiles: Seq[File],
    dependencyClasspath: ClassPath,
    scaladocTarget: File,
    compileArgs: Seq[String],
    mavenCache: File
  )(implicit transientCache: java.util.Map[AnyRef,AnyRef], classLoaderCache: ClassLoaderCache): Option[File] = {
    if(sourceFiles.isEmpty){
      None
    } else {
      scaladocTarget.mkdirs
      val args = Seq(
        // FIXME: can we use compiler dependency here?
        "-cp", dependencyClasspath.string, // FIXME: does this break for builds that don't have scalac dependencies?
        "-d",  scaladocTarget.toString
      ) ++ compileArgs ++ sourceFiles.map(_.toString)
      logger.lib("creating docs for source files "+args.mkString(", "))
      redirectOutToErr{
        new ScalaDependencies(cbtLastModified,mavenCache,scalaVersion).runMain(
          "scala.tools.nsc.ScalaDoc", args
        )
      }
      Some(scaladocTarget)
    }
  }

  // task reflection helpers
  def taskMethods[T: universe.TypeTag](cls: Class[T]) = {
    implicit val ct = ClassTag[T](cls)
    val mirror = universe.runtimeMirror(cls.getClassLoader)

    def declaredIn(tpe: universe.Type) = {
      tpe
        .decls
        .collect {
          case m if
          !m.isConstructor &&
            m.isPublic &&
            m.isMethod &&
            !m.name.decodedName.toString.contains("$") &&
            m.asMethod.paramLists.forall(_.isEmpty) =>

            val methodF: AnyRef => AnyRef = instance => mirror.reflect(instance).reflectMethod(m.asMethod).apply().asInstanceOf[AnyRef]
            m.name.decodedName.toString -> methodF
        }
        .toMap
    }

    mirror
      .typeOf[T]
      .baseClasses
      .dropRight(2) // drop … <: Object <: Any
      .foldLeft(Map.empty[String, AnyRef => AnyRef]) {
      case (map, symbol) =>
        map ++ declaredIn(symbol.typeSignature)
    }
  }

  def taskNames[T: universe.TypeTag](cls: Class[T]): Seq[String] = taskMethods(cls).keys.toVector.sorted

  def usage(buildClass: Class[_], show: String): String = {
    val baseTasks = Seq(
      classOf[BasicBuild],
      classOf[PackageJars],
      classOf[Publish]
    ).flatMap(lib.taskNames(_)).distinct.sorted
    val thisTasks = lib.taskNames(buildClass) diff baseTasks
    (
      s"Methods provided by $show\n\n"
      ++ (
        if( thisTasks.nonEmpty ){
          thisTasks.mkString("  ") ++ "\n\n"
        } else "<none>"
      )
      ++ s"\n\nMethods provided by CBT (but possibly overwritten)\n\n"
      ++ baseTasks.mkString("  ") + "\n"
    )
  }

  def callReflective[T <: AnyRef]( obj: T, code: Option[String], context: Context ): ExitCode = {
    val result = getReflective( obj, code, f => {
      def g( a: AnyRef):  AnyRef = a match {
        case bs: Seq[_] if bs.size > 0 && bs.forall(_.isInstanceOf[BaseBuild]) =>
          bs.map(_.asInstanceOf[BaseBuild]).map(f)
        case obj: LazyDependency => g(obj.dependency)
        case obj => f(obj)
      }
      g(_)
    } )( context )

    val applied = result.getClass.getMethods.find(m => m.getName == "apply" && m.getParameterCount == 0).map(
      _.invoke( result )
    ).getOrElse( result )

    applied match {
      case e: ExitCode => e
      case s: Seq[_] =>
        val v = render(applied)
        if(v.nonEmpty) System.out.println(v)
        s.collect{ case e: ExitCode => e }.reduceOption(_ && _).getOrElse( ExitCode.Success )
      case other =>
        val s = render(applied)
        if(s.nonEmpty) System.out.println(s)
        ExitCode.Success
    }
  }

  private def render( obj: Any ): String = {
    obj match {
      case Some(s) => render(s)
      case None => ""
      case url: URL => url.show // to remove credentials
      case d: Dependency => lib.usage(d.getClass, d.show())
      case c: ClassPath => c.string
      //case e: ExitCode => System.err.println(e.integer); System.exit(e.integer); ???
      case s: Seq[_] => s.map(render).mkString("\n")
      case s: Set[_] => s.map(render).toSeq.sorted.mkString("\n")
      case null => "null"
      case _ => obj.toString
    }
  }

  def getReflective[T <: AnyRef](
    obj: T, code: Option[String], callback: (AnyRef => AnyRef) => AnyRef => AnyRef = f => f(_)
  )( implicit context: Context ) =
    callInternal( obj, code.toSeq.flatMap(_.split("\\.").map( NameTransformer.encode )), Nil, context, callback )

  private def callInternal[T <: AnyRef](
    _obj: T, members: Seq[String], previous: Seq[String], context: Context,
    callback: (AnyRef => AnyRef) => AnyRef => AnyRef
  ): Object = {
    callback{ obj =>
      members.headOption.map{ taskName =>
        val name = NameTransformer.decode(taskName)
        logger.lib("Calling task " ++ taskName.toString)
        taskMethods(obj.getClass).get(name).map{ method =>
          callInternal(
            Option(trapExitCodeOrValue(method(obj)).merge /* null in case of Unit */ ).getOrElse(
              ().asInstanceOf[AnyRef]
            ),
            members.tail,
            previous :+ taskName,
            context,
            callback
          )
        }.getOrElse{
          if( context =!= null && (context.workingDirectory / name).exists ){
            val newContext = context.copy( workingDirectory = context.workingDirectory / name )
            val build = DirectoryDependency( newContext, None ).dependency
            callInternal( build, members.tail, previous :+ taskName, newContext, callback )
          } else {
            val p = previous.mkString(".")
            val msg = (if(p.nonEmpty) p ++ s" has " else " ")
            throw new RuntimeException( msg ++ lib.red(s"no method `$name` in\n") + render(obj) )
          }
        }
      }.getOrElse( obj )
    }(_obj)
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
          file.listOrFail.toVector.flatMap(depthFirstFileStream(_))
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
          System.err.println("Files to be deleted:\n")
          files.foreach( System.err.println )
          System.err.println("")
          System.err.print("To delete the above files type 'delete': ")
          new BufferedReader( new InputStreamReader(System.in), 1 ).readLine == "delete"
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

  /** Which file endings to consider being source files. */
  def sourceFileFilter(file: File): Boolean = file.toString.endsWith(".scala") || file.toString.endsWith(".java")

  def sourceFiles( sources: Seq[File], sourceFileFilter: File => Boolean = sourceFileFilter ): Seq[File] = {
    for {
      base <- sources.filter(_.exists).map(lib.realpath)
      file <- base.listRecursive if file.isFile && sourceFileFilter(file)
    } yield file
  }

  private def createManifest( mainClass: Option[String] ) = {
    val m = new Manifest()
    m.getMainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0")
    m.getMainAttributes.putValue( "Created-By", "Scala CBT" )
    mainClass.foreach(
      m.getMainAttributes.put(Attributes.Name.MAIN_CLASS, _)
    )
    m
  }

  def createJar( jarFile: File, files: Seq[File], mainClass: Option[String] = None ): Option[File] = {
    deleteIfExists(jarFile.toPath)
    if( files.isEmpty ){
      None
    } else {
      jarFile.getParentFile.mkdirs
      logger.lib("Start packaging "++jarFile.string)
      val jar = new JarOutputStream(new FileOutputStream(jarFile), createManifest(mainClass))
      try{
        assert( files.forall(_.exists) )
        autoRelative(files).collect{
          case (file, relative) if file.isFile =>
            val entry = new JarEntry( relative )
            entry.setTime(file.lastModified)
            jar.putNextEntry(entry)
            jar.write( readAllBytes( file.toPath ) )
            jar.closeEntry
        }
      } finally {
        jar.close
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

  def publishUnsigned( artifacts: Seq[File], url: URL, credentials: Option[String] = None ): Seq[URL] = {
    publish( artifacts, url, credentials )
  }

  def publishLocal( artifacts: Seq[File], mavenCache: File, releaseFolder: String ): Seq[File] = {
    val targetDir = mavenCache ++ releaseFolder.stripSuffix("/")
    targetDir.mkdirs
    artifacts.map{ a =>
      val target = targetDir ++ ("/" ++ a.getName)
      System.err.println(blue("publishing ") ++ target.getPath)
      Files.copy( a.toPath, target.toPath, StandardCopyOption.REPLACE_EXISTING )
      target
    }
  }

  def publishSigned( artifacts: Seq[File], url: URL, credentials: Option[String] = None ): Seq[URL] = {
    publish( artifacts ++ artifacts.map(sign), url, credentials )
  }

  private def publish(artifacts: Seq[File], url: URL, credentials: Option[String]): Seq[URL] = {
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

  def uploadAll(url: URL, nameAndContents: Seq[(String, Array[Byte])], credentials: Option[String] = None ): Seq[URL] =
    nameAndContents.map{ case(name, content) => upload(name, content, url, credentials ) }

  def upload(fileName: String, fileContents: Array[Byte], baseUrl: URL, credentials: Option[String] = None): URL = {
    import java.net._
    import java.io._
    val url = baseUrl ++ "/" ++ fileName
    System.err.println(blue("uploading ") ++ url.show)
    val httpCon = Stage0Lib.openConnectionConsideringProxy(url)
    try{
      httpCon.setDoOutput(true)
      httpCon.setRequestMethod("PUT")
      (credentials orElse Option(baseUrl.getUserInfo)).foreach(addHttpCredentials(httpCon,_))
      httpCon.setRequestProperty("Content-Type", "application/binary")
      httpCon.getOutputStream.write(
        fileContents
      )
      httpCon.getInputStream
    } finally {
      httpCon.disconnect
    }
    url
  }

  def findOuterMostModuleDirectory(directory: File): File = {
    if(
      ( directory.getParentFile ++ ("/" ++ lib.buildDirectoryName) ).exists
    ) findOuterMostModuleDirectory(directory.getParentFile) else directory
  }

  def clearScreen = System.err.println( (27.toChar +: "[2J").mkString )
}
