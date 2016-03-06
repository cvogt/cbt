package cbt
import cbt.paths._

import java.io._
import java.net._
import java.lang.reflect.InvocationTargetException
import java.nio.file.{Path =>_,_}
import java.nio.file.Files.readAllBytes
import java.security.MessageDigest
import java.util.jar._

import scala.collection.immutable.Seq
import scala.reflect.runtime.{universe => ru}
import scala.util._

import ammonite.ops.{cwd => _,_}

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
    def findStartDir(cwd: String): String = {    
      val buildDir = realpath(cwd+"/build")
      if(new File(buildDir).exists) findStartDir(buildDir) else cwd
    }

    val start = findStartDir(context.cwd)

    val useBasicBuildBuild = context.cwd == start

    val rootBuildClassName = if( useBasicBuildBuild ) buildBuildClassName else buildClassName
    try{
      if(useBasicBuildBuild) default( context ) else new cbt.BuildBuild( context.copy( cwd = start ) )
    } catch {
      case e:ClassNotFoundException if e.getMessage == rootBuildClassName =>
        throw new Exception(s"no class $rootBuildClassName found in "+start)
    }
  }

  def compile(
    updated: Boolean,
    sourceFiles: Seq[File], compileTarget: File, dependenyClasspath: ClassPath,
    compileArgs: Seq[String], zincVersion: String, scalaVersion: String
  ): File = {    
    if(sourceFiles.nonEmpty)
      lib.zinc(
        updated, sourceFiles, compileTarget, dependenyClasspath, compileArgs
      )( zincVersion = zincVersion, scalaVersion = scalaVersion )
    compileTarget
  }
  
  def getClassFiles(target : File, classLoader : ClassLoader) : List[File]  = { 
    def hasMainClass(file : File) : Boolean = {
      val className = getClassName(file)
  
      val inspectClass = classLoader.loadClass(className)
      inspectClass.getDeclaredMethods().map(_.getName).contains("main")
    }

    def isInnerClass(file : File) : Boolean = file.toString.contains("$")
    val files = target.listFiles

    val classFiles = files.filter(file => file.toString.endsWith(".class") && !isInnerClass(file)).toList

    classFiles.filter(hasMainClass(_))
	}
  
  def pickMain(files : List[File]) : File = {
    val mains = files.map(_.getName).zipWithIndex map {case (fileName, index) => s"""[${index + 1}] ${fileName}\n"""}
    println(s"""Multiple main classes detected. Select one to run:
        
    ${mains.mkString("\t\n")}""")

    //FIXME: doesn't stop to read the line!!!
    //val choice = scala.io.StdIn.readLine() 

    files(0) // defaulting to first file in the meantime
  }
  
  def getClassName(file : File) = file.getName.takeWhile(_ != '.')
  
  def getRunClass(compileTarget : File, classLoader : ClassLoader) : String = {
    val hasMain = getClassFiles(compileTarget, classLoader)

    val classFile = if (hasMain.length == 1) hasMain(0) else pickMain(hasMain) 

    getClassName(classFile)
  }

  def srcJar(sources: Seq[File], artifactId: String, version: String, jarTarget: File): File = {
    val file = new File(jarTarget+"/"+artifactId+"-"+version+"-sources.jar")
    lib.jarFile(file, sources)
    file
  }

  def jar(artifactId: String, version: String, compileTarget: File, jarTarget: File): File = {
    val file = new File(jarTarget+"/"+artifactId+"-"+version+".jar")
    lib.jarFile(file, Seq(compileTarget))
    file
  }

  def docJar(
    sourceFiles: Seq[File],
    dependenyClasspath: ClassPath,
    apiTarget: File,
    jarTarget: File,
    artifactId: String,
    version: String,
    compileArgs: Seq[String]
  ): File = {
    class DisableSystemExit extends Exception
    object DisableSystemExit{
      def apply(e: Throwable): Boolean = {
        e match {
          case i: InvocationTargetException => apply(i.getTargetException)
          case _: DisableSystemExit => true
          case _ => false
        }
      }
    }

    // FIXME: get this dynamically somehow, or is this even needed?
    val javacp = ClassPath(
      "/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/ext/cldrdata.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/ext/dnsns.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/ext/jaccess.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/ext/jfxrt.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/ext/localedata.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/ext/nashorn.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/ext/sunec.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/ext/sunjce_provider.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/ext/sunpkcs11.jar:/Library/Java/JavaVirtualMachines/jdk1.8.0_60.jdk/Contents/Home/jre/lib/ext/zipfs.jar:/System/Library/Java/Extensions/MRJToolkit.jar".split(":").toVector.map(new File(_))
    )

    mkdir(Path(apiTarget))
    if(sourceFiles.nonEmpty){    
      System.err.println("creating docs")
      try{    
        System.setSecurityManager(
          new SecurityManager{
            override def checkPermission( permission: java.security.Permission ) = {
              if( permission.getName.startsWith("exitVM") ) throw new DisableSystemExit
            }
          }
        )
        redirectOutToErr{        
          runMain(
            "scala.tools.nsc.ScalaDoc",
            Seq(
              // FIXME: can we use compiler dependency here?
              "-cp", /*javacp+":"+*/ScalaDependencies(logger).classpath.string + ":" + dependenyClasspath.string,
              "-d",  apiTarget.toString
            ) ++ compileArgs ++ sourceFiles.map(_.toString),
            new URLClassLoader(
              ScalaDependencies(logger).classpath ++ javacp,
              ClassLoader.getSystemClassLoader
            )
          )
        }
      } catch {
        case e:InvocationTargetException if DisableSystemExit(e) =>
      } finally {
        System.setSecurityManager(null)
      }
    }
    val docJar = new File(jarTarget+"/"+artifactId+"-"+version+"-javadoc.jar")
    lib.jarFile(docJar, Vector(apiTarget))
    docJar
  }

  def test( context: Context ) = {
    logger.lib(s"invoke testDefault( $context )")
    loadDynamic(
      context.copy( cwd = context.cwd+"/test/" ),
      new Build(_) with mixins.Test
    ).run
    logger.lib(s"return testDefault( $context )")
    
  }

  // task reflection helpers
  import ru._
  private lazy val anyRefMembers = ru.typeOf[AnyRef].members.toVector.map(taskName)
  def taskNames(tpe: Type) = tpe.members.toVector.flatMap(lib.toTask).map(taskName).sorted
  private def taskName(method: Symbol) = method.name.decodedName.toString
  def toTask(symbol: Symbol): Option[MethodSymbol] = {
    Option(symbol)
      .filter(_.isPublic)
      .filter(_.isMethod)
      .map(_.asMethod)
      .filter(_.paramLists.flatten.size == 0)
      .filterNot(taskName(_) contains "$")
      .filterNot(t => anyRefMembers contains taskName(t))
  }

  class ReflectBuild(build: Build) extends ReflectObject(build){
    def usage = {
      val baseTasks = lib.taskNames(ru.typeOf[Build])
      val thisTasks = lib.taskNames(subclassType) diff baseTasks
      (
        (
          if( thisTasks.nonEmpty ){
            s"""Methods provided by Build ${build.context.cwd}

  ${thisTasks.mkString("  ")}

"""
          } else ""
        ) + s"""Methods provided by CBT (but possibly overwritten)

  ${baseTasks.mkString("  ")}"""
      ) + "\n"
    }
  }

  abstract class ReflectObject[T:scala.reflect.ClassTag](obj: T){
    lazy val mirror = ru.runtimeMirror(obj.getClass.getClassLoader)
    lazy val subclassType = mirror.classSymbol(obj.getClass).toType
    def usage: String
    def callNullary( taskName: Option[String] ): Unit = {
      taskName
        .map{ n => subclassType.member(ru.TermName(n).encodedName) }
        .filter(_ != ru.NoSymbol)
        .flatMap(toTask _)
        .map{ methodSymbol =>
          val result = mirror.reflect(obj).reflectMethod(methodSymbol)()
          // Try to render console representation. Probably not the best way to do this.
          val method = scala.util.Try( result.getClass.getDeclaredMethod("toConsole") )
          
          method.foreach(m => println(m.invoke(result)))
          method.recover{
            case e:NoSuchMethodException if e.getMessage contains "toConsole" =>
              result match {
                case () => ""
                case other => println( other.toString ) // no method .toConsole, using to String
              }
          }
        }.getOrElse{
          taskName.foreach{ n =>
            System.err.println(s"Method not found: $n")
            System.err.println("")
          }
          System.err.println(usage)
          System.exit(1)
        }
    }
  }


  // file system helpers
  def basename(path: String) = path.stripSuffix("/").split("/").last
  def basename(path: File) = path.toString.stripSuffix("/").split("/").last
  def dirname(path: String) = realpath(path).stripSuffix("/").split("/").dropRight(1).mkString("/")
  def realpath(name: String) = Paths.get(new File(name).getAbsolutePath).normalize.toString
  def realpath(name: File) = new File(Paths.get(name.getAbsolutePath).normalize.toString)
  def nameAndContents(file: File) = basename(file.toString) -> readAllBytes(Paths.get(file.toString))

  def jarFile( jarFile: File, files: Seq[File] ): Unit = {
    logger.lib("Start packaging "+jarFile)
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
        jar.write( Files.readAllBytes( Paths.get(file.toString) ) )
        jar.closeEntry
        name
    }
    
    val duplicateFiles = (names diff names.distinct).distinct
    assert(
      duplicateFiles.isEmpty,
      s"Conflicting file names when trying to create $jarFile: "+duplicateFiles.mkString(", ")
    )

    jar.close
    logger.lib("Done packaging "+jarFile)
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
    
    if( 0 != statusCode ) throw new Exception("gpg exited with status code "+statusCode)

    new File(file+".asc")
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
    val path = new File(jarTarget+"/"+artifactId+"-"+version+".pom")
    write.over(Path(path), "<?xml version='1.0' encoding='UTF-8'?>\n" + xml.toString)
    path
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
          name+".md5" -> md5(content).toArray.map(_.toByte),
          name+".sha1" -> sha1(content).toArray.map(_.toByte)
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
    logger.task("uploading "+fileName)
    val url = new URL(
      baseUrl + fileName
    )
    val httpCon = url.openConnection.asInstanceOf[HttpURLConnection]
    httpCon.setDoOutput(true)
    httpCon.setRequestMethod("PUT")
    val userPassword = read(Path(sonatypeLogin)).trim
    val encoding = new sun.misc.BASE64Encoder().encode(userPassword.getBytes)
    httpCon.setRequestProperty("Authorization", "Basic " + encoding)
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

    files.map{
      file =>
      if(file.isFile) new File( dirname(file.toString) )
      else file
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
        Option(watcher.take).map{
          key =>
          val changedFiles = key
            .pollEvents
            .filterNot(_.kind == StandardWatchEventKind.OVERFLOW)
            .map(_.context.toString)
            .map(new File(_))
          changedFiles.foreach( f => logger.loop("Changed: "+f) )
          changedFiles.collect(action)
          key.reset
        }
      }
    }
  }
}
