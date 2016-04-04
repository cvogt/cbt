package cbt
import java.nio.file._
import java.nio.charset.StandardCharsets
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

  def needsUpdate: Boolean
  //def cacheClassLoader: Boolean = false
  private[cbt] def targetClasspath: ClassPath
  def exportedClasspath: ClassPath
  def exportedJars: Seq[File]
  def jars: Seq[File] = exportedJars ++ dependencyJars

  def canBeCached: Boolean

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
            case m: BoundMavenDependency => latest( (m.groupId,m.artifactId) )
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

  private def actual(current: Dependency, latest: Map[(String,String),Dependency]) = current match {
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
        new cbt.URLClassLoader( a.exportedClasspath, dependencyClassLoader(latest, cache) )
      )
    } else {
      new cbt.URLClassLoader( exportedClasspath, dependencyClassLoader(latest, cache) )
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
    noInfo ++ BoundMavenDependency.updateOutdated( hasInfo ).reverse.distinct
  }

  def show: String = this.getClass.getSimpleName
  // ========== debug ==========
  def dependencyTree: String = dependencyTreeRecursion()
  private def dependencyTreeRecursion(indent: Int = 0): String = (
    ( " " * indent )
    ++ (if(needsUpdate) lib.red(show) else show)
    ++ dependencies.map(
      "\n" ++ _.dependencyTreeRecursion(indent + 1)
    ).mkString
  )
}

// TODO: all this hard codes the scala version, needs more flexibility
class ScalaCompilerDependency(version: String)(implicit logger: Logger) extends BoundMavenDependency(MavenDependency("org.scala-lang","scala-compiler",version, Classifier.none), Seq(MavenRepository.central.url))
class ScalaLibraryDependency (version: String)(implicit logger: Logger) extends BoundMavenDependency(MavenDependency("org.scala-lang","scala-library",version, Classifier.none), Seq(MavenRepository.central.url))
class ScalaReflectDependency (version: String)(implicit logger: Logger) extends BoundMavenDependency(MavenDependency("org.scala-lang","scala-reflect",version, Classifier.none), Seq(MavenRepository.central.url))

case class ScalaDependencies(version: String)(implicit val logger: Logger) extends Dependency{ sd =>
  override final val needsUpdate = false
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

case class BinaryDependency( path: File, dependencies: Seq[Dependency], canBeCached: Boolean )(implicit val logger: Logger) extends Dependency{
  def exportedClasspath = ClassPath(Seq(path))
  def exportedJars = Seq[File](path)
  override def needsUpdate = false
  def targetClasspath = exportedClasspath
}

/** Allows to easily assemble a bunch of dependencies */
case class Dependencies( dependencies: Seq[Dependency] )(implicit val logger: Logger) extends Dependency{
  override def needsUpdate = dependencies.exists(_.needsUpdate)
  override def canBeCached = dependencies.forall(_.canBeCached)
  override def exportedClasspath = ClassPath(Seq())
  override def exportedJars = Seq()
  override def targetClasspath = ClassPath(Seq())
}
object Dependencies{
  def apply( dependencies: Dependency* )(implicit logger: Logger): Dependencies = Dependencies( dependencies.to )
}

case class Stage1Dependency()(implicit val logger: Logger) extends Dependency{
  override def needsUpdate = false // FIXME: think this through, might allow simplifications and/or optimizations
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
  override def targetClasspath = exportedClasspath
  override def exportedClasspath = ClassPath( Seq(nailgunTarget, stage1Target) )
  override def exportedJars = ???//Seq[File]()  
  override def dependencies = Seq(
    MavenRepository.central.resolve(
      MavenDependency("org.scala-lang","scala-library",constants.scalaVersion),
      MavenDependency("org.scala-lang.modules","scala-xml_"+constants.scalaMajorVersion,"1.0.5")
    )
  )
  // FIXME: implement sanity check to prevent using incompatible scala-library and xml version on cp
  override def classLoaderRecursion( latest: Map[(String,String),Dependency], cache: ClassLoaderCache )
    = getClass.getClassLoader
}
case class CbtDependency()(implicit val logger: Logger) extends Dependency{
  override def needsUpdate = false // FIXME: think this through, might allow simplifications and/or optimizations
  override def canBeCached = false
  override def targetClasspath = exportedClasspath
  override def exportedClasspath = ClassPath( Seq( stage2Target ) )
  override def exportedJars = ???
  override def dependencies = Seq(
    Stage1Dependency(),
    MavenRepository.central.resolve(
      MavenDependency("net.incongru.watchservice","barbary-watchservice","1.0"),
      MavenDependency("org.eclipse.jgit", "org.eclipse.jgit", "4.2.0.201601211800-r")
    )
  )
}

case class Classifier(name: Option[String])
object Classifier{
  object none extends Classifier(None)
  object javadoc extends Classifier(Some("javadoc"))
  object sources extends Classifier(Some("sources"))
}
abstract class DependenciesProxy{

}
class BoundMavenDependencies(
  urls: Seq[URL], mavenDependencies: Seq[MavenDependency]
)(implicit logger: Logger) extends Dependencies(
  mavenDependencies.map( BoundMavenDependency(_,urls) )
)
case class MavenDependency(
  groupId: String, artifactId: String, version: String, classifier: Classifier = Classifier.none
)
case class BoundMavenDependency(
  mavenDependency: MavenDependency, repositories: Seq[URL]
)(implicit val logger: Logger) extends ArtifactInfo{
  val MavenDependency( groupId, artifactId, version, classifier ) = mavenDependency
  assert(
    Option(groupId).collect{
      case BoundMavenDependency.ValidIdentifier(_) =>
    }.nonEmpty,
    s"not a valid groupId: '$groupId'"
  )
  assert(
    Option(artifactId).collect{
      case BoundMavenDependency.ValidIdentifier(_) =>
    }.nonEmpty,
    s"not a valid artifactId: '$artifactId'"
  )
  assert(
    version != "" && version != null && !version.startsWith(" ") && !version.endsWith(" "),
    s"not a valid version: '$version'"
  )

  override def needsUpdate = false
  override def canBeCached = dependencies.forall(_.canBeCached)

  private val groupPath = groupId.split("\\.").mkString("/")
  protected[cbt] def basePath = s"/$groupPath/$artifactId/$version/$artifactId-$version" ++ classifier.name.map("-"++_).getOrElse("")
  
  //private def coursierJarFile = userHome++"/.coursier/cache/v1/https/repo1.maven.org/maven2"++basePath++".jar"

  override def exportedJars = Seq( jar )
  override def exportedClasspath = ClassPath( exportedJars )
  override def targetClasspath = exportedClasspath
  import scala.collection.JavaConversions._

  private def resolve(suffix: String, hash: Option[String]): File = {
    val file = mavenCache ++ basePath ++ "." ++ suffix
    val urls = repositories.map(_ ++ basePath ++ "." ++ suffix)
    urls.find(
      lib.download(_, file, hash)
    ).getOrElse(
      throw new Exception(s"\nCannot resolve\n$this\nCan't find any of\n"++urls.mkString("\n"))
    )
    file
  }

  private def resolveHash(suffix: String) = {
    Files.readAllLines(
      resolve( suffix ++ ".sha1", None ).toPath,
      StandardCharsets.UTF_8
    ).mkString("\n").split(" ").head.trim
  }
  
  private object jarSha1Cache extends Cache[String]
  def jarSha1: String = jarSha1Cache{ resolveHash("jar") }

  private object pomSha1Cache extends Cache[String]
  def pomSha1: String = pomSha1Cache{ resolveHash("pom") }

  private object jarCache extends Cache[File]
  def jar: File = jarCache{ resolve("jar", Some(jarSha1)) }

  private object pomCache extends Cache[File]
  def pom: File = pomCache{ resolve("pom", Some(pomSha1)) }

  def pomXml = XML.loadFile(pom.string)
  // ========== pom traversal ==========

  lazy val transitivePom: Seq[BoundMavenDependency] = {
    (pomXml \ "parent").collect{
      case parent =>
        BoundMavenDependency(
          MavenDependency(
            (parent \ "groupId").text,
            (parent \ "artifactId").text,
            (parent \ "version").text
          ),
          repositories
        )(logger)
    }.flatMap(_.transitivePom) :+ this
  }

  lazy val properties: Map[String, String] = (
    transitivePom.flatMap{ d =>
      val props = (d.pomXml \ "properties").flatMap(_.child).map{
        tag => tag.label -> tag.text
      }
      logger.pom(s"Found properties in $pom: $props")
      props
    }
  ).toMap

  lazy val dependencyVersions: Map[String, (String,String)] =
    transitivePom.flatMap(
      p =>
      (p.pomXml \ "dependencyManagement" \ "dependencies" \ "dependency").map{
        xml =>
          val groupId = p.lookup(xml,_ \ "groupId").get
          val artifactId = p.lookup(xml,_ \ "artifactId").get
          val version = p.lookup(xml,_ \ "version").get
          artifactId -> (groupId, version)
      }
    ).toMap

  def dependencies: Seq[BoundMavenDependency] = {
    if(classifier == Classifier.sources) Seq()
    else (pomXml \ "dependencies" \ "dependency").collect{
      case xml if (xml \ "scope").text == "" && (xml \ "optional").text != "true" =>
        val artifactId = lookup(xml,_ \ "artifactId").get
        val groupId =
          lookup(xml,_ \ "groupId").getOrElse(
            dependencyVersions
              .get(artifactId).map(_._1)
              .getOrElse(
                throw new Exception(s"$artifactId not found in \n$dependencyVersions")
              )
          )
        val version =
          lookup(xml,_ \ "version").getOrElse(
            dependencyVersions
              .get(artifactId).map(_._2)
              .getOrElse(
                throw new Exception(s"$artifactId not found in \n$dependencyVersions")
              )
          )
        BoundMavenDependency(
          MavenDependency(
            groupId, artifactId, version,
            Classifier( Some( (xml \ "classifier").text ).filterNot(_ == "").filterNot(_ == null) )
          ),
          repositories
        )
    }.toVector
  }
  def lookup( xml: Node, accessor: Node => NodeSeq ): Option[String] = {
    //println("lookup in "++pomUrl)
    val Substitution = "\\$\\{([^\\}]+)\\}".r
    accessor(xml).headOption.flatMap{v =>
      //println("found: "++v.text)
      v.text match {
        case Substitution(path) => Option(
          properties.get(path).orElse(
            transitivePom.reverse.flatMap{ d =>
              Some(path.split("\\.").toList).collect{
                case "project" :: path =>
                  path.foldLeft(d.pomXml:NodeSeq){ case (xml,tag) => xml \ tag }.text
              }.filter(_ != "")
            }.headOption
          )
          .getOrElse( throw new Exception(s"Can't find $path in \n$properties.\n\npomParents: $transitivePom\n\n pomXml:\n$pomXml" )))
          //println("lookup "++path ++ ": "++(pomXml\path).text)          
        case value => Option(value)
      }
    }
  }
}
object BoundMavenDependency{
  def ValidIdentifier = "^([A-Za-z0-9_\\-.]+)$".r // according to maven's DefaultModelValidator.java
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
