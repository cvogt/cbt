package cbt
import java.nio.file._
import java.net._
import java.io._
import scala.collection.immutable.Seq
import scala.xml._
import paths._
import scala.concurrent._
import scala.concurrent.duration._

private final class Tree( val root: Dependency, computeChildren: => Seq[Tree] ){
  lazy val children = computeChildren
  def linearize: Seq[Dependency] = root +: children.flatMap(_.linearize)
  def show(indent: Int = 0): Stream[Char] = {
    ("  " * indent ++ root.show ++ "\n").toStream #::: children.map(_.show(indent+1)).foldLeft(Stream.empty[Char])(_ #::: _)
  }
}

trait ArtifactInfo extends Dependency{
  def artifactId: String
  def groupId: String
  def version: String

  protected def str = s"$groupId:$artifactId:$version"
  override def show = super.show ++ s"($str)"
}
abstract class Dependency{
  implicit def logger: Logger
  protected def lib = new Stage1Lib(logger)

  def updated: Boolean
  //def cacheClassLoader: Boolean = false
  private[cbt] def targetClasspath: ClassPath
  def exportedClasspath: ClassPath
  def exportedJars: Seq[File]
  def jars: Seq[File] = exportedJars ++ dependencyJars

  def canBeCached = false

  //private type BuildCache = KeyLockedLazyCache[Dependency, Future[ClassPath]]
  def exportClasspathConcurrently: ClassPath = {
    // FIXME: this should separate a blocking and a non-blocking EC
    import scala.concurrent.ExecutionContext.Implicits.global
    Await.result(
      exportClasspathConcurrently(
        transitiveDependencies
          .collect{ case d: ArtifactInfo => d }
          .groupBy( d => (d.groupId,d.artifactId) )
          .mapValues( _.head )
        //, new BuildCache
      ),
      Duration.Inf
    )
  }

  def concurrencyEnabled = false

  /**
  The implementation of this method is untested and likely buggy
  at this stage.
  */
  private object cacheExportClasspathConcurrently extends Cache[Future[ClassPath]]
  private def exportClasspathConcurrently(
    latest: Map[(String, String),ArtifactInfo]//, cache: BuildCache
  )( implicit ec: ExecutionContext ): Future[ClassPath] = cacheExportClasspathConcurrently{
    Future.sequence( // trigger compilation / download of all dependencies first
      this.dependencies.map{
        d =>
          // find out latest version of the required dependency
          val l = d match {
            case m: JavaDependency => latest( (m.groupId,m.artifactId) )
            case _ => d
          }
          // // trigger compilation if not already triggered
          // cache.get( l, l.exportClasspathConcurrently( latest, cache ) )
          l.exportClasspathConcurrently( latest )
      }
    ).map(
      // merge dependency classpaths into one
      ClassPath.flatten(_)
    ).map(
      _ =>
      // now that all dependencies are done, compile the code of this
      exportedClasspath
    )
  }

  def actual(current: Dependency, latest: Map[(String,String),Dependency]) = current match {
    case d: ArtifactInfo => latest((d.groupId,d.artifactId))
    case d => d
  }
  private def dependencyClassLoader( latest: Map[(String,String),Dependency], cache: ClassLoaderCache ): ClassLoader = {
    if( dependencies.isEmpty ){
      ClassLoader.getSystemClassLoader
    } else if( dependencies.size == 1 ){
      dependencies.head.classLoaderRecursion( latest, cache )
    } else if( dependencies.forall(_.canBeCached) ){
      assert(transitiveDependencies.forall(_.canBeCached))
      cache.persistent.get(
        dependencyClasspath.string,
        new MultiClassLoader(
          dependencies.map( _.classLoaderRecursion(latest, cache) )
        )
      )
    } else {
      val (cachable, nonCachable) = dependencies.partition(_.canBeCached)
      new URLClassLoader(
        ClassPath.flatten( nonCachable.map(actual(_,latest)).map(_.exportedClasspath) ),
        cache.persistent.get(
          ClassPath.flatten( cachable.map(actual(_,latest)).map(_.exportedClasspath) ).string,
          new MultiClassLoader(
            cachable.map( _.classLoaderRecursion(latest, cache) )
          )
        )
      )
      new MultiClassLoader(
        dependencies.map( _.classLoaderRecursion(latest, cache) )
      )
    }
  }
  protected def classLoaderRecursion( latest: Map[(String,String),Dependency], cache: ClassLoaderCache ): ClassLoader = {
    if( canBeCached ){
      val a = actual( this, latest )
      cache.persistent.get(
        a.classpath.string,
        cbt.URLClassLoader( a.exportedClasspath, dependencyClassLoader(latest, cache) )
      )
    } else {
      cbt.URLClassLoader( exportedClasspath, dependencyClassLoader(latest, cache) )
    }
  }
  private object classLoaderCache extends Cache[ClassLoader]
  def classLoader( cache: ClassLoaderCache  ): ClassLoader = classLoaderCache{
    if( concurrencyEnabled ){
      // trigger concurrent building / downloading dependencies
      exportClasspathConcurrently
    }
    classLoaderRecursion(
      (this +: transitiveDependencies).collect{
        case d: ArtifactInfo => d 
      }.groupBy(
        d => (d.groupId,d.artifactId)
      ).mapValues(_.head),
      cache
    )
  }
  def classpath           : ClassPath = exportedClasspath ++ dependencyClasspath
  def dependencyJars      : Seq[File] = transitiveDependencies.flatMap(_.jars)
  def dependencyClasspath : ClassPath = ClassPath.flatten( transitiveDependencies.map(_.exportedClasspath) )
  def dependencies: Seq[Dependency]

  private def resolveRecursive(parents: List[Dependency] = List()): Tree = {
    // diff removes circular dependencies
    new Tree(this, (dependencies diff parents).map(_.resolveRecursive(this :: parents)))
  }

  def linearize(deps: Seq[Dependency]): Seq[Dependency] =
    if(deps.isEmpty) deps else ( deps ++ linearize(deps.flatMap(_.dependencies)) )
  
  private object transitiveDependenciesCache extends Cache[Seq[Dependency]]
  /** return dependencies in order of linearized dependence. this is a bit tricky. */
  def transitiveDependencies: Seq[Dependency] = transitiveDependenciesCache{
    val deps = linearize(dependencies)
    val hasInfo = deps.collect{ case d:ArtifactInfo => d }
    val noInfo  = deps.filter{
      case _:ArtifactInfo => false
      case _ => true
    }
    noInfo ++ JavaDependency.updateOutdated( hasInfo ).reverse.distinct
  }

  def show: String = this.getClass.getSimpleName
  // ========== debug ==========
  def dependencyTree: String = dependencyTreeRecursion()
  private def dependencyTreeRecursion(indent: Int = 0): String = (
    ( " " * indent )
    ++ (if(updated) lib.red(show) else show)
    ++ dependencies.map(
      "\n" ++ _.dependencyTreeRecursion(indent + 1)
    ).mkString
  )
}

// TODO: all this hard codes the scala version, needs more flexibility
class ScalaCompilerDependency(version: String)(implicit logger: Logger) extends JavaDependency("org.scala-lang","scala-compiler",version)
class ScalaLibraryDependency (version: String)(implicit logger: Logger) extends JavaDependency("org.scala-lang","scala-library",version)
class ScalaReflectDependency (version: String)(implicit logger: Logger) extends JavaDependency("org.scala-lang","scala-reflect",version)

case class ScalaDependencies(version: String)(implicit val logger: Logger) extends Dependency{ sd =>
  final val updated = false
  override def canBeCached = true
  def targetClasspath = ClassPath(Seq())
  def exportedClasspath = ClassPath(Seq())
  def exportedJars = Seq[File]()  
  def dependencies = Seq(
    new ScalaCompilerDependency(version),
    new ScalaLibraryDependency(version),
    new ScalaReflectDependency(version)
  )
}

case class BinaryDependency( path: File, dependencies: Seq[Dependency] )(implicit val logger: Logger) extends Dependency{
  def updated = false
  def exportedClasspath = ClassPath(Seq(path))
  def exportedJars = Seq[File](path)
  def targetClasspath = exportedClasspath
}

/** Allows to easily assemble a bunch of dependencies */
case class Dependencies( _dependencies: Dependency* )(implicit val logger: Logger) extends Dependency{
  override def dependencies = _dependencies.to
  def updated = dependencies.exists(_.updated)
  def exportedClasspath = ClassPath(Seq())
  def exportedJars = Seq()
  def targetClasspath = ClassPath(Seq())
}

case class Stage1Dependency()(implicit val logger: Logger) extends Dependency{
  def updated = false // FIXME: think this through, might allow simplifications and/or optimizations
  override def canBeCached = false
  /*
  private object classLoaderRecursionCache extends Cache[ClassLoader]
  override def classLoaderRecursion(latest: Map[(String,String),Dependency], cache: ClassLoaderCache) = classLoaderRecursionCache{
    println(System.currentTimeMillis)
    val cl = getClass.getClassLoader
    println(System.currentTimeMillis)
    cl
    ClassLoader.getSystemClassLoader
  }
  */
  override def exportedClasspath = ClassPath(Seq(nailgunTarget, stage1Target) )
  override def exportedJars = ???//Seq[File]()  
  override def dependencies = ScalaDependencies(constants.scalaVersion).dependencies
  def targetClasspath = exportedClasspath
}
case class CbtDependency()(implicit val logger: Logger) extends Dependency{
  override def canBeCached = false
  override def exportedClasspath = ClassPath( Seq( stage2Target ) )
  override def exportedJars = Seq[File]()  
  override def dependencies = Seq(
    Stage1Dependency(),
    JavaDependency("net.incongru.watchservice","barbary-watchservice","1.0"),
    JavaDependency("org.eclipse.jgit", "org.eclipse.jgit", "4.2.0.201601211800-r"),
    lib.ScalaDependency(
      "org.scala-lang.modules","scala-xml","1.0.5", scalaVersion = constants.scalaMajorVersion
    )
  )
  def updated = false // FIXME: think this through, might allow simplifications and/or optimizations
  def targetClasspath = exportedClasspath
}

case class Classifier(name: Option[String])
object Classifier{
  object none extends Classifier(None)
  object javadoc extends Classifier(Some("javadoc"))
  object sources extends Classifier(Some("sources"))
}

case class JavaDependency(
  groupId: String, artifactId: String, version: String, classifier: Classifier = Classifier.none
)(implicit val logger: Logger) extends ArtifactInfo{

  def updated = false
  override def canBeCached = true

  private val groupPath = groupId.split("\\.").mkString("/")
  def basePath = s"/$groupPath/$artifactId/$version/$artifactId-$version" ++ classifier.name.map("-"++_).getOrElse("")
  
  private def resolverUrl:URL = new URL(
    if(version.endsWith("-SNAPSHOT")) "https://oss.sonatype.org/content/repositories/snapshots" else "https://repo1.maven.org/maven2"
  )
  private def baseUrl: URL = resolverUrl ++ basePath
  private def baseFile: File = mavenCache ++ basePath
  private def pomFile: File = baseFile ++ ".pom"
  private def jarFile: File = baseFile ++ ".jar"
  //private def coursierJarFile = userHome++"/.coursier/cache/v1/https/repo1.maven.org/maven2"++basePath++".jar"
  private def pomUrl: URL = baseUrl ++ ".pom"
  private[cbt] def jarUrl: URL = baseUrl ++ ".jar"    

  def exportedJars = Seq( jar )
  def exportedClasspath = ClassPath( exportedJars )
  def targetClasspath = exportedClasspath
  import scala.collection.JavaConversions._
  
  def jarSha1 = {
    val file = jarFile ++ ".sha1"
    lib.download( jarUrl ++ ".sha1"  , file, None )
    // split(" ") here so checksum file contents in this format work: df7f15de037a1ee4d57d2ed779739089f560338c  jna-3.2.2.pom
    Files.readAllLines(Paths.get(file.string)).mkString("\n").split(" ").head.trim
  }

  def pomSha1 = {
    val file = pomFile++".sha1"
    lib.download( pomUrl++".sha1" , file, None )
    // split(" ") here so checksum file contents in this format work: df7f15de037a1ee4d57d2ed779739089f560338c  jna-3.2.2.pom
    Files.readAllLines(Paths.get(file.string)).mkString("\n").split(" ").head.trim
  }

  private object jarCache extends Cache[File]
  def jar = jarCache{
    lib.download( jarUrl, jarFile, Some(jarSha1) )
    jarFile
  }
  def pomXml = XML.loadFile(pom.toString)

  def pom = {
    lib.download( pomUrl, pomFile, Some(pomSha1) )
    pomFile
  }

  // ========== pom traversal ==========

  lazy val pomParents: Seq[JavaDependency] = {
    (pomXml \ "parent").collect{
      case parent =>
        JavaDependency(
          (parent \ "groupId").text,
          (parent \ "artifactId").text,
          (parent \ "version").text
        )(logger)
    }
  }

  lazy val properties: Map[String, String] = (
    pomParents.flatMap(_.properties) ++ {
      val props = (pomXml \ "properties").flatMap(_.child).map{
        tag => tag.label -> tag.text
      }
      logger.pom(s"Found properties in $pom: $props")
      props
    }
  ).toMap

  lazy val dependencyVersions: Map[(String,String), String] =
    pomParents.flatMap(
      p =>
      p.dependencyVersions
      ++ 
      (p.pomXml \ "dependencyManagement" \ "dependencies" \ "dependency").map{
        xml =>
          val groupId = p.lookup(xml,_ \ "groupId").get
          val artifactId = p.lookup(xml,_ \ "artifactId").get
          val version = p.lookup(xml,_ \ "version").get
          (groupId, artifactId) -> version
      }
    ).toMap

  def dependencies: Seq[JavaDependency] = {
    if(classifier == Classifier.sources) Seq()
    else (pomXml \ "dependencies" \ "dependency").collect{
      case xml if (xml \ "scope").text == "" && (xml \ "optional").text != "true" =>
        val groupId = lookup(xml,_ \ "groupId").get
        val artifactId = lookup(xml,_ \ "artifactId").get
        JavaDependency(
          groupId,
          artifactId,
          lookup(xml,_ \ "version").getOrElse( dependencyVersions(groupId, artifactId) ),
          Classifier( Some( (xml \ "classifier").text ).filterNot(_ == "").filterNot(_ == null) )
        )(logger)
    }.toVector
  }
  def lookup( xml: Node, accessor: Node => NodeSeq ): Option[String] = {
    //println("lookup in "++pomUrl)
    val Substitution = "\\$\\{([^\\}]+)\\}".r
    accessor(xml).headOption.flatMap{v =>
      //println("found: "++v.text)
      v.text match {
        case Substitution(path) => Option(properties(path))
          //println("lookup "++path ++ ": "++(pomXml\path).text)          
        case value => Option(value)
      }
    }
  }
}
object JavaDependency{
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
  def updateOutdated(
    deps: Seq[ArtifactInfo],
    versionLessThan: (String, String) => Boolean = semanticVersionLessThan
  )(implicit logger: Logger): Seq[ArtifactInfo] = {
    val latest = deps
      .groupBy( d => (d.groupId, d.artifactId) )
      .mapValues(
        _.sortBy( _.version )( Ordering.fromLessThan(versionLessThan) )
         .last
      )
    deps.map{
      d => 
        val l = latest((d.groupId,d.artifactId))
        if(d != l) logger.resolver("outdated: "++d.show)
        l
    }
  }
}
