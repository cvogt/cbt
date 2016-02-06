package cbt
import java.nio.file._
import java.net._
import java.io._
import scala.collection.immutable.Seq
import scala.xml._
import paths._

private final class Tree( val root: Dependency, computeChildren: => Seq[Tree] ){
  lazy val children = computeChildren
  def linearize: Seq[Dependency] = root +: children.flatMap(_.linearize)
  def show(indent: Int = 0): Stream[Char] = {
    ("  " * indent + root.show + "\n").toStream #::: children.map(_.show(indent+1)).foldLeft(Stream.empty[Char])(_ #::: _)
  }
}

trait ArtifactInfo extends Dependency{
  def artifactId: String
  def groupId: String
  def version: String

  protected def str = s"$groupId:$artifactId:$version"
  override def show = super.show + s"($str)"
}
abstract class Dependency{

  def updated: Boolean
  //def cacheClassLoader: Boolean = false
  def exportedClasspath: ClassPath
  def exportedJars: Seq[File]
  def jars: Seq[File] = exportedJars ++ dependencyJars

  def cacheDependencyClassLoader = true

  private object cacheClassLoaderBasicBuild extends Cache[URLClassLoader]
  def classLoader: URLClassLoader = cacheClassLoaderBasicBuild{
    val transitiveClassPath = transitiveDependencies.map{
      case d: MavenDependency => Left(d)
      case d => Right(d)
    }
    val buildClassPath = ClassPath.flatten(
      transitiveClassPath.flatMap(
        _.right.toOption.map(_.exportedClasspath)
      )
    )
    val mavenClassPath = ClassPath.flatten(
      transitiveClassPath.flatMap(
        _.left.toOption
      ).par.map(_.exportedClasspath).seq.sortBy(_.string)
    )
    if(cacheDependencyClassLoader){    
      val mavenClassPathKey = mavenClassPath.strings.sorted.mkString(":")
      new URLClassLoader(
        exportedClasspath ++ buildClassPath,
        ClassLoaderCache.classLoader(
          mavenClassPathKey, new URLClassLoader( mavenClassPath, ClassLoader.getSystemClassLoader )
        )
      )
    } else {
      new URLClassLoader(
        exportedClasspath ++ buildClassPath ++ mavenClassPath, ClassLoader.getSystemClassLoader
      )
    }
  }
  def classpath           : ClassPath = exportedClasspath ++ dependencyClasspath
  def dependencyJars      : Seq[File] = transitiveDependencies.flatMap(_.jars)
  def dependencyClasspath : ClassPath = ClassPath.flatten( transitiveDependencies.map(_.exportedClasspath) )
  def dependencies: Seq[Dependency]

  private def resolveRecursive(parents: List[Dependency] = List()): Tree = {
    // diff removes circular dependencies
    new Tree(this, (dependencies diff parents).map(_.resolveRecursive(this :: parents)))
  }

  def transitiveDependencies: Seq[Dependency] = {
    val deps = dependencies.flatMap(_.resolveRecursive().linearize)
    val hasInfo = deps.collect{ case d:ArtifactInfo => d }
    val noInfo  = deps.filter{
      case _:ArtifactInfo => false
      case _ => true
    }
    noInfo ++ MavenDependency.removeOutdated( hasInfo )
  }

  def show: String = this.getClass.getSimpleName
  // ========== debug ==========
  def dependencyTree: String = dependencyTreeRecursion()
  def logger: Logger
  protected def lib = new Stage1Lib(logger)
  private def dependencyTreeRecursion(indent: Int = 0): String = ( " " * indent ) + (if(updated) lib.red(show) else show) + dependencies.map(_.dependencyTreeRecursion(indent + 1)).map("\n"+_).mkString("")

  private object cacheDependencyClassLoaderBasicBuild extends Cache[ClassLoader]
}

// TODO: all this hard codes the scala version, needs more flexibility
class ScalaCompiler(logger: Logger) extends MavenDependency("org.scala-lang","scala-compiler",constants.scalaVersion)(logger)
class ScalaLibrary(logger: Logger) extends MavenDependency("org.scala-lang","scala-library",constants.scalaVersion)(logger)
class ScalaReflect(logger: Logger) extends MavenDependency("org.scala-lang","scala-reflect",constants.scalaVersion)(logger)

case class ScalaDependencies(logger: Logger) extends Dependency{
  def exportedClasspath = ClassPath(Seq())
  def exportedJars = Seq[File]()  
  def dependencies = Seq( new ScalaCompiler(logger), new ScalaLibrary(logger), new ScalaReflect(logger) )
  final val updated = false
}

/*
case class BinaryDependency( path: File, dependencies: Seq[Dependency] ) extends Dependency{
  def exportedClasspath = ClassPath(Seq(path))
  def exportedJars = Seq[File]()
}
*/

case class Stage1Dependency(logger: Logger) extends Dependency{
  def exportedClasspath = ClassPath( Seq(nailgunTarget, stage1Target) )
  def exportedJars = Seq[File]()  
  def dependencies = ScalaDependencies(logger: Logger).dependencies
  def updated = false // FIXME: think this through, might allow simplifications and/or optimizations
}
case class CbtDependency(logger: Logger) extends Dependency{
  def exportedClasspath = ClassPath( Seq( stage2Target ) )
  def exportedJars = Seq[File]()  
  override def dependencies = Seq(
    Stage1Dependency(logger),
    MavenDependency("net.incongru.watchservice","barbary-watchservice","1.0")(logger),
    MavenDependency("com.lihaoyi","ammonite-repl_2.11.7","0.5.5")(logger),
    MavenDependency("org.scala-lang.modules","scala-xml_2.11","1.0.5")(logger)
  )
  def updated = false // FIXME: think this through, might allow simplifications and/or optimizations
}

sealed trait ClassifierBase
final case class Classifier(name: String) extends ClassifierBase
case object javadoc extends ClassifierBase
case object sources extends ClassifierBase

case class MavenDependency( groupId: String, artifactId: String, version: String, sources: Boolean = false )(val logger: Logger)
  extends ArtifactInfo{

  def updated = false

  private val groupPath = groupId.split("\\.").mkString("/")
  def basePath = s"/$groupPath/$artifactId/$version/$artifactId-$version"+(if(sources) "-sources" else "")
  
  private def resolverUrl = if(version.endsWith("-SNAPSHOT")) "https://oss.sonatype.org/content/repositories/snapshots" else "https://repo1.maven.org/maven2"
  private def baseUrl = resolverUrl + basePath
  private def baseFile = mavenCache + basePath
  private def pomFile = baseFile+".pom"
  private def jarFile = baseFile+".jar"
  //private def coursierJarFile = userHome+"/.coursier/cache/v1/https/repo1.maven.org/maven2"+basePath+".jar"
  private def pomUrl = baseUrl+".pom"
  private def jarUrl = baseUrl+".jar"    

  def exportedJars = Seq( jar )
  def exportedClasspath = ClassPath( exportedJars )

  import scala.collection.JavaConversions._
  
  def jarSha1 = {
    val file = jarFile+".sha1"
    def url = jarUrl+".sha1"    
    scala.util.Try{
      lib.download( new URL(url), Paths.get(file), None )
      // split(" ") here so checksum file contents in this format work: df7f15de037a1ee4d57d2ed779739089f560338c  jna-3.2.2.pom
      Files.readAllLines(Paths.get(file)).mkString("\n").split(" ").head.trim
    }.toOption // FIXME: .toOption is a temporary solution to ignore if libs don't have one
  }
  def pomSha1 = {
    val file = pomFile+".sha1"
    def url = pomUrl+".sha1"    
    scala.util.Try{ 
      lib.download( new URL(url), Paths.get(file), None )
      // split(" ") here so checksum file contents in this format work: df7f15de037a1ee4d57d2ed779739089f560338c  jna-3.2.2.pom
      Files.readAllLines(Paths.get(file)).mkString("\n").split(" ").head.trim
    }.toOption // FIXME: .toOption is a temporary solution to ignore if libs don't have one
  }
  def jar = {
    lib.download( new URL(jarUrl), Paths.get(jarFile), jarSha1 )
    new File(jarFile)
  }
  def pomXml = {
    XML.loadFile(pom.toString)
  }
  def pom = {
    lib.download( new URL(pomUrl), Paths.get(pomFile), pomSha1 )
    new File(pomFile)
  }

  // ========== pom traversal ==========

  lazy val pomParents: Seq[MavenDependency] = {
    (pomXml \ "parent").collect{
      case parent =>
        MavenDependency(
          (parent \ "groupId").text,
          (parent \ "artifactId").text,
          (parent \ "version").text
        )(logger)
    }
  }
  def dependencies: Seq[MavenDependency] = {
    if(sources) Seq()
    else (pomXml \ "dependencies" \ "dependency").collect{
      case xml if (xml \ "scope").text == "" && (xml \ "optional").text != "true" =>
        MavenDependency(
          lookup(xml,_ \ "groupId").get,
          lookup(xml,_ \ "artifactId").get,
          lookup(xml,_ \ "version").get,
          (xml \ "classifier").text == "sources"
        )(logger)
    }.toVector
  }
  def lookup( xml: Node, accessor: Node => NodeSeq ): Option[String] = {
    //println("lookup in "+pomUrl)
    val Substitution = "\\$\\{([a-z0-9\\.]+)\\}".r
    accessor(xml).headOption.flatMap{v =>
      //println("found: "+v.text)
      v.text match {
        case Substitution(path) =>
          //println("lookup "+path + ": "+(pomXml\path).text)
          lookup(pomXml, _ \ "properties" \ path)
        case value => Option(value)
      }
    }.orElse(
      pomParents.map(p => p.lookup(p.pomXml, accessor)).flatten.headOption
    )
  }
}
object MavenDependency{
  def semanticVersionLessThan(left: String, right: String) = {
    // FIXME: this ignores ends when different size
    val zipped = left.split("\\.|\\-").map(toInt) zip right.split("\\.|\\-").map(toInt)
    val res = zipped.map {
      case (Left(i),Left(j)) => i compare j
      case (Right(i),Right(j)) => i compare j
      case (Left(i),Right(j)) => i.toString compare j
      case (Right(i),Left(j)) => i compare j.toString
    }
    res.find(_ != 0).map(_ < 0).getOrElse(false)
  }
  def toInt(str: String): Either[Int,String] = try {
    Left(str.toInt)
  } catch {
    case e: NumberFormatException => Right(str)
  }
  /* this obviously should be overridable somehow */
  def removeOutdated(
    deps: Seq[ArtifactInfo],
    versionLessThan: (String, String) => Boolean = semanticVersionLessThan
  ): Seq[ArtifactInfo] = {
    val latest = deps
      .groupBy( d => (d.groupId, d.artifactId) )
      .mapValues(
        _.sortBy( _.version )( Ordering.fromLessThan(versionLessThan) )
         .last
      )
    deps.flatMap{
      d => 
        val l = latest.get((d.groupId,d.artifactId))
        //if(d != l) println("EVICTED: "+d.show)
        l
    }.distinct
  }
}
