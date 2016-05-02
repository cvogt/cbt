package cbt
import java.nio.file._
import java.nio.charset.StandardCharsets
import java.net._
import java.io._
import scala.collection.immutable.Seq
import scala.xml._
import scala.concurrent._
import scala.concurrent.duration._

abstract class DependencyImplementation extends Dependency{
  implicit protected def logger: Logger
  protected def lib = new Stage1Lib(logger)

  def needsUpdate: Boolean
  //def cacheClassLoader: Boolean = false
  private[cbt] def targetClasspath: ClassPath
  def dependencyClasspathArray: Array[File] = dependencyClasspath.files.toArray
  def exportedClasspathArray: Array[File] = exportedClasspath.files.toArray
  def exportedClasspath: ClassPath
  def dependenciesArray: Array[Dependency] = dependencies.to

  def needsUpdateCompat: java.lang.Boolean = needsUpdate

  /*
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
      ), // FIXME
      Duration.Inf
    )
  }

  def concurrencyEnabled = false

  /**
  The implementation of this method is untested and likely buggy
  at this stage.
  */
  def exportClasspathConcurrently(
    latest: Map[(String, String),Dependency with ArtifactInfo]//, cache: BuildCache
  )( implicit ec: ExecutionContext ): Future[AnyRef] = {
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
          l.exportClasspathConcurrently( latest ) // FIXME
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
  */

  def classLoader( cache: ClassLoaderCache ): ClassLoader = {
    /*
    if( concurrencyEnabled ){
      // trigger concurrent building / downloading dependencies
      exportClasspathConcurrently
    }
    */
    lib.classLoaderRecursion(
      this,
      (this +: transitiveDependencies).collect{
        case d: ArtifactInfo => d 
      }.groupBy(
        d => (d.groupId,d.artifactId)
      ).mapValues(_.head),
      cache // FIXME
    )
  }
  // FIXME: these probably need to update outdated as well
  def classpath           : ClassPath = exportedClasspath ++ dependencyClasspath
  def dependencyClasspath : ClassPath = ClassPath(
    transitiveDependencies
    .flatMap(_.exportedClasspath.files)
    .distinct // <- currently needed here to handle diamond dependencies on builds (duplicate in classpath)
  )
  def dependencies: Seq[Dependency]

  private object transitiveDependenciesCache extends Cache[Seq[Dependency]]
  /** return dependencies in order of linearized dependence. this is a bit tricky. */
  def transitiveDependencies: Seq[Dependency] = transitiveDependenciesCache{
    lib.transitiveDependencies(this)
  }

  override def show: String = this.getClass.getSimpleName
  // ========== debug ==========
  def dependencyTree: String = lib.dependencyTreeRecursion(this)
}

// TODO: all this hard codes the scala version, needs more flexibility
class ScalaCompilerDependency(cbtHasChanged: Boolean, mavenCache: File, version: String)(implicit logger: Logger) extends BoundMavenDependency(cbtHasChanged, mavenCache, MavenDependency("org.scala-lang","scala-compiler",version, Classifier.none), Seq(mavenCentral))
class ScalaLibraryDependency (cbtHasChanged: Boolean, mavenCache: File, version: String)(implicit logger: Logger) extends BoundMavenDependency(cbtHasChanged, mavenCache, MavenDependency("org.scala-lang","scala-library",version, Classifier.none), Seq(mavenCentral))
class ScalaReflectDependency (cbtHasChanged: Boolean, mavenCache: File, version: String)(implicit logger: Logger) extends BoundMavenDependency(cbtHasChanged, mavenCache, MavenDependency("org.scala-lang","scala-reflect",version, Classifier.none), Seq(mavenCentral))

case class ScalaDependencies(cbtHasChanged: Boolean, mavenCache: File, version: String)(implicit val logger: Logger) extends DependencyImplementation{ sd =>
  override final val needsUpdate = false
  def targetClasspath = ClassPath(Seq())
  def exportedClasspath = ClassPath(Seq())
  def dependencies = Seq(
    new ScalaCompilerDependency(cbtHasChanged, mavenCache, version),
    new ScalaLibraryDependency(cbtHasChanged, mavenCache, version),
    new ScalaReflectDependency(cbtHasChanged, mavenCache, version)
  )
}

case class BinaryDependency( path: File, dependencies: Seq[Dependency] )(implicit val logger: Logger) extends DependencyImplementation{
  def exportedClasspath = ClassPath(Seq(path))
  override def needsUpdate = false
  def targetClasspath = exportedClasspath
}

/** Allows to easily assemble a bunch of dependencies */
case class Dependencies( dependencies: Seq[Dependency] )(implicit val logger: Logger) extends DependencyImplementation{
  override def needsUpdate = dependencies.exists(_.needsUpdate)
  override def exportedClasspath = ClassPath(Seq())
  override def targetClasspath = ClassPath(Seq())
}
object Dependencies{
  def apply( dependencies: Dependency* )(implicit logger: Logger): Dependencies = Dependencies( dependencies.to )
}

case class Stage1Dependency(cbtHasChanged: Boolean, mavenCache: File, nailgunTarget: File, stage1Target: File, compatibilityTarget: File)(implicit val logger: Logger) extends DependencyImplementation{
  override def needsUpdate = cbtHasChanged
  override def targetClasspath = exportedClasspath
  override def exportedClasspath = ClassPath( Seq(nailgunTarget, stage1Target) )
  val compatibilityDependency = CompatibilityDependency(cbtHasChanged, compatibilityTarget)
  override def dependencies = Seq(
    compatibilityDependency
  ) ++
  MavenResolver(cbtHasChanged,mavenCache,mavenCentral).bind(
    MavenDependency("org.scala-lang","scala-library",constants.scalaVersion),
    MavenDependency("org.scala-lang.modules","scala-xml_"+constants.scalaMajorVersion,constants.scalaXmlVersion)
  )
}
case class CompatibilityDependency(cbtHasChanged: Boolean, compatibilityTarget: File)(implicit val logger: Logger) extends DependencyImplementation{
  override def needsUpdate = cbtHasChanged
  override def targetClasspath = exportedClasspath
  override def exportedClasspath = ClassPath( Seq(compatibilityTarget) )
  override def dependencies = Seq()
}
case class CbtDependency(cbtHasChanged: Boolean, mavenCache: File, nailgunTarget: File, stage1Target: File, stage2Target: File, compatibilityTarget: File)(implicit val logger: Logger) extends DependencyImplementation{
  override def needsUpdate = cbtHasChanged
  override def targetClasspath = exportedClasspath
  override def exportedClasspath = ClassPath( Seq( stage2Target ) )
  val stage1Dependency = Stage1Dependency(cbtHasChanged, mavenCache, nailgunTarget, stage1Target, compatibilityTarget)
  override def dependencies = Seq(
    stage1Dependency
  ) ++ 
  MavenResolver(cbtHasChanged, mavenCache,mavenCentral).bind(
    MavenDependency("net.incongru.watchservice","barbary-watchservice","1.0"),
    MavenDependency("org.eclipse.jgit", "org.eclipse.jgit", "4.2.0.201601211800-r")
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
  cbtHasChanged: Boolean, mavenCache: File, urls: Seq[URL], mavenDependencies: Seq[MavenDependency]
)(implicit logger: Logger) extends Dependencies(
  mavenDependencies.map( BoundMavenDependency(cbtHasChanged,mavenCache,_,urls) )
)
case class MavenDependency(
  groupId: String, artifactId: String, version: String, classifier: Classifier = Classifier.none
){
  private[cbt] def serialize = groupId ++ ":" ++ artifactId ++ ":"++ version ++ ":" ++ classifier.name.getOrElse("")
}
object MavenDependency{
  private[cbt] def deserialize = (_:String).split(":") match {
    case col => MavenDependency( col(0), col(1), col(2), Classifier(col.lift(3)) )
  }
}
// FIXME: take MavenResolver instead of mavenCache and repositories separately
case class BoundMavenDependency(
  cbtHasChanged: Boolean, mavenCache: File, mavenDependency: MavenDependency, repositories: Seq[URL]
)(implicit val logger: Logger) extends DependencyImplementation with ArtifactInfo{
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

  private val groupPath = groupId.split("\\.").mkString("/")
  protected[cbt] def basePath = s"/$groupPath/$artifactId/$version/$artifactId-$version" ++ classifier.name.map("-"++_).getOrElse("")
  
  //private def coursierJarFile = userHome++"/.coursier/cache/v1/https/repo1.maven.org/maven2"++basePath++".jar"

  def exportedJars = Seq( jar )
  override def exportedClasspath = ClassPath( exportedJars )
  override def targetClasspath = exportedClasspath
  import scala.collection.JavaConversions._

  private def resolve(suffix: String, hash: Option[String]): File = {
    logger.resolver("Resolving "+this)
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

  private def pomXml = XML.loadFile(pom.string)
  // ========== pom traversal ==========

  private lazy val transitivePom: Seq[BoundMavenDependency] = {
    (pomXml \ "parent").collect{
      case parent =>
        BoundMavenDependency(
          cbtHasChanged: Boolean,
          mavenCache,
          MavenDependency(
            (parent \ "groupId").text,
            (parent \ "artifactId").text,
            (parent \ "version").text
          ),
          repositories
        )(logger)
    }.flatMap(_.transitivePom) :+ this
  }

  private lazy val properties: Map[String, String] = (
    transitivePom.flatMap{ d =>
      val props = (d.pomXml \ "properties").flatMap(_.child).map{
        tag => tag.label -> tag.text
      }
      logger.pom(s"Found properties in $pom: $props")
      props
    }
  ).toMap

  private lazy val dependencyVersions: Map[String, (String,String)] =
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
    else {
      lib.cacheOnDisk(
        cbtHasChanged, mavenCache ++ basePath ++ ".pom.dependencies"
      )( MavenDependency.deserialize )( _.serialize ){
        (pomXml \ "dependencies" \ "dependency").collect{
          case xml if ( (xml \ "scope").text == "" || (xml \ "scope").text == "compile" ) && (xml \ "optional").text != "true" =>
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
            val classifier = Classifier( Some( (xml \ "classifier").text ).filterNot(_ == "").filterNot(_ == null) )
            MavenDependency( groupId, artifactId, version, classifier )
        }.toVector
      }.map(
        BoundMavenDependency( cbtHasChanged, mavenCache, _, repositories )
      ).to
    }
  }
  def lookup( xml: Node, accessor: Node => NodeSeq ): Option[String] = {
    //println("lookup in "++pomUrl)
    val Substitution = "\\$\\{([^\\}]+)\\}".r
    accessor(xml).headOption.map{v =>
      //println("found: "++v.text)
      Substitution.replaceAllIn(
        v.text,
        matcher => {
          val path = matcher.group(1)
          properties.get(path).orElse(
            transitivePom.reverse.flatMap{ d =>
              Some(path.split("\\.").toList).collect{
                case "project" :: path =>
                  path.foldLeft(d.pomXml:NodeSeq){ case (xml,tag) => xml \ tag }.text
              }.filter(_ != "")
            }.headOption
          )
          .getOrElse(
            throw new Exception(s"Can't find $path in \n$properties.\n\npomParents: $transitivePom\n\n pomXml:\n$pomXml" )
          )
            //println("lookup "++path ++ ": "++(pomXml\path).text)          
        }
      )
    }
  }
}
object BoundMavenDependency{
  def ValidIdentifier = "^([A-Za-z0-9_\\-.]+)$".r // according to maven's DefaultModelValidator.java
  def semanticVersionLessThan(left: Array[Either[Int,String]], right: Array[Either[Int,String]]) = {
    // FIXME: this ignores ends when different size
    val zipped = left zip right
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
    deps: Seq[Dependency with ArtifactInfo],
    versionLessThan: (Array[Either[Int,String]], Array[Either[Int,String]]) => Boolean = semanticVersionLessThan
  )(implicit logger: Logger): Seq[Dependency with ArtifactInfo] = {
    val latest = deps
      .groupBy( d => (d.groupId, d.artifactId) )
      .mapValues(
        _.groupBy(_.version) // remove duplicates
         .map( _._2.head )
         .toVector
         .sortBy( _.version.split("\\.|\\-").map(toInt) )( Ordering.fromLessThan(versionLessThan) )
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
