package cbt
import java.nio.file._
import java.nio.charset.StandardCharsets
import java.net._
import java.io._
import scala.xml._

trait DependencyImplementation extends Dependency{
  implicit protected def logger: Logger
  protected def lib = new Stage1Lib(logger)
  implicit protected def transientCache: java.util.Map[AnyRef,AnyRef]
  implicit protected def classLoaderCache: ClassLoaderCache

  /** key used by taskCache to identify different objects that represent the same logical module */
  protected def moduleKey: String
  /**
   caches given value in context keyed with given key and projectDirectory
   the context is fresh on every complete run of cbt
   */
  protected lazy val taskCache = new PerClassCache(transientCache, moduleKey)

  private[cbt] def targetClasspath: ClassPath
  def exportedClasspathArray: Array[File] = exportedClasspath.files.toArray
  def exportedClasspath: ClassPath
  def dependenciesArray: Array[Dependency] = dependencies.to

  @deprecated("this method was replaced by lastModifiedCompat","")
  def needsUpdateCompat = true
  @deprecated("this method was replaced by dependenciesArray","")
  def dependencyClasspathArray = dependencyClasspath.files.toArray


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

  def runMain( className: String, args: String* ) = lib.runMain( className, args, classLoader )

  def flatClassLoader: Boolean = false

  def mainClasses: Seq[Class[_]] = exportedClasspath.files.flatMap( lib.mainClasses( _, classLoader ) )

  def runClass: Option[String] = lib.runClass( mainClasses ).map( _.getName )

  def run( args: String* ): ExitCode = {
    runClass.map( runMain( _, args: _* ) ).getOrElse{
      // FIXME: this just doing nothing when class is not found has been repeatedly
      // surprising. Let's try to make this more visible than just logging an error.
      // Currently blocked on task `recursive` trying every subbuild and would error
      // for all that don't have a run class. Maybe that's ok actually.
      logger.task( "No main class found for " ++ show )
      ExitCode.Success
    }
  }

  def classLoader: ClassLoader = {
    if( flatClassLoader ){
      new java.net.URLClassLoader(classpath.strings.map(f => new URL("file://" ++ f)).toArray)
    } else {
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
        ).mapValues(_.head)
      )
    }
  }

  // FIXME: these probably need to update outdated as well
  def classpath           : ClassPath = exportedClasspath ++ dependencyClasspath
  def dependencyClasspath : ClassPath = ClassPath(
    transitiveDependencies
    .flatMap(_.exportedClasspath.files)
    .distinct // <- currently needed here to handle diamond dependencies on builds (duplicate in classpath)
  )
  def dependencies: Seq[Dependency]

  /** return dependencies in order of linearized dependence. this is a bit tricky. */
  def transitiveDependencies: Seq[Dependency] =
    taskCache[DependencyImplementation]( "transitiveDependencies" ).memoize{
      lib.transitiveDependencies(this)
    }

  override def show: String = this.getClass.getSimpleName
  // ========== debug ==========
  def dependencyTree: String = lib.dependencyTreeRecursion(this)
}

// TODO: all this hard codes the scala version, needs more flexibility
class ScalaCompilerDependency(cbtLastModified: Long, mavenCache: File, version: String)(implicit logger: Logger, transientCache: java.util.Map[AnyRef,AnyRef], classLoaderCache: ClassLoaderCache) extends BoundMavenDependency(cbtLastModified, mavenCache, MavenDependency("org.scala-lang","scala-compiler",version, Classifier.none), Seq(mavenCentral))
class ScalaLibraryDependency (cbtLastModified: Long, mavenCache: File, version: String)(implicit logger: Logger, transientCache: java.util.Map[AnyRef,AnyRef], classLoaderCache: ClassLoaderCache) extends BoundMavenDependency(cbtLastModified, mavenCache, MavenDependency("org.scala-lang","scala-library",version, Classifier.none), Seq(mavenCentral))
class ScalaReflectDependency (cbtLastModified: Long, mavenCache: File, version: String)(implicit logger: Logger, transientCache: java.util.Map[AnyRef,AnyRef], classLoaderCache: ClassLoaderCache) extends BoundMavenDependency(cbtLastModified, mavenCache, MavenDependency("org.scala-lang","scala-reflect",version, Classifier.none), Seq(mavenCentral))

class ScalaDependencies(cbtLastModified: Long, mavenCache: File, version: String)(implicit logger: Logger, transientCache: java.util.Map[AnyRef,AnyRef], classLoaderCache: ClassLoaderCache) extends Dependencies(
  Seq(
    new ScalaCompilerDependency(cbtLastModified, mavenCache, version),
    new ScalaLibraryDependency(cbtLastModified, mavenCache, version),
    new ScalaReflectDependency(cbtLastModified, mavenCache, version)
  )
)

case class BinaryDependency( paths: Seq[File], dependencies: Seq[Dependency] )(implicit val logger: Logger, val transientCache: java.util.Map[AnyRef,AnyRef], val classLoaderCache: ClassLoaderCache) extends DependencyImplementation{
  assert(paths.nonEmpty)
  def exportedClasspath = ClassPath(paths)
  override def lastModified = paths.map(_.lastModified).maxOption.getOrElse(0) // FIXME: cache this
  def targetClasspath = exportedClasspath
  def moduleKey = this.getClass.getName ++ "(" ++ paths.mkString(", ") ++ ")"
}

/** Allows to easily assemble a bunch of dependencies */
case class Dependencies( dependencies: Seq[Dependency] )(implicit val logger: Logger, val transientCache: java.util.Map[AnyRef,AnyRef], val classLoaderCache: ClassLoaderCache) extends DependencyImplementation{
  override def lastModified = dependencies.map(_.lastModified).maxOption.getOrElse(0)
  def moduleKey = this.getClass.getName ++ "(" ++ dependencies.map(_.moduleKey).mkString(", ") ++ ")"
  def targetClasspath = ClassPath() 
  def exportedClasspath = ClassPath() 
  override def show: String = this.getClass.getSimpleName + "( " + dependencies.map(_.show).mkString(", ") + " )"
}

case class PostBuildDependency(target: File, _dependencies: Seq[DependencyImplementation])(implicit val logger: Logger, val transientCache: java.util.Map[AnyRef,AnyRef], val classLoaderCache: ClassLoaderCache) extends DependencyImplementation{
  override final lazy val lastModified = (target++".last-success").lastModified
  def moduleKey = target.string
  override def targetClasspath = exportedClasspath
  override def exportedClasspath = ClassPath( Seq(target) )
  override def dependencies = _dependencies
}
case class CbtDependencies(mavenCache: File, nailgunTarget: File, stage1Target: File, stage2Target: File, compatibilityTarget: File)(implicit logger: Logger, transientCache: java.util.Map[AnyRef,AnyRef], classLoaderCache: ClassLoaderCache){
  val compatibilityDependency = PostBuildDependency(compatibilityTarget, Nil)
  val cbtLastModified = (stage2Target++".last-success").lastModified
  val stage1Dependency = PostBuildDependency(
    stage1Target,
    Seq(
      PostBuildDependency(nailgunTarget, Nil),
      compatibilityDependency
    ) ++
    MavenResolver(cbtLastModified,mavenCache,mavenCentral).bind(
      MavenDependency("org.scala-lang","scala-library",constants.scalaVersion),
      MavenDependency("org.scala-lang.modules","scala-xml_"+constants.scalaMajorVersion,constants.scalaXmlVersion)
    )
  )
  val stage2Dependency = PostBuildDependency(
    stage2Target,
    stage1Dependency +:
    MavenResolver(cbtLastModified, mavenCache,mavenCentral).bind(
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
  cbtLastModified: Long, mavenCache: File, urls: Seq[URL], mavenDependencies: Seq[MavenDependency]
)(
  implicit logger: Logger, transientCache: java.util.Map[AnyRef,AnyRef], classLoaderCache: ClassLoaderCache
) extends Dependencies(
  mavenDependencies.map( BoundMavenDependency(cbtLastModified,mavenCache,_,urls) )
)
case class MavenDependency(
  groupId: String, artifactId: String, version: String, classifier: Classifier = Classifier.none
){
  private[cbt] def serialize = groupId ++ ":" ++ artifactId ++ ":"++ version ++ classifier.name.map(":" ++ _).getOrElse("")
}
object MavenDependency{
  private[cbt] def deserialize = (_:String).split(":") match {
    case col => MavenDependency( col(0), col(1), col(2), Classifier(col.lift(3)) )
  }
}
// FIXME: take MavenResolver instead of mavenCache and repositories separately
case class BoundMavenDependency(
  cbtLastModified: Long, mavenCache: File, mavenDependency: MavenDependency, repositories: Seq[URL]
)(
  implicit val logger: Logger, val transientCache: java.util.Map[AnyRef,AnyRef], val classLoaderCache: ClassLoaderCache
) extends ArtifactInfo with DependencyImplementation{
  def moduleKey = this.getClass.getName ++ "(" ++ mavenDependency.serialize ++ ")"
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
  override def show: String = this.getClass.getSimpleName ++ "(" ++ mavenDependency.serialize ++ ")"

  override final lazy val lastModified = classpath.strings.map(new File(_).lastModified).max

  private val groupPath = groupId.split("\\.").mkString("/")
  protected[cbt] def basePath(useClassifier: Boolean) = s"/$groupPath/$artifactId/$version/$artifactId-$version" ++ (if (useClassifier) classifier.name.map("-"++_).getOrElse("") else "")

  //private def coursierJarFile = userHome++"/.coursier/cache/v1/https/repo1.maven.org/maven2"++basePath++".jar"

  def exportedJars = Seq( jar )
  override def exportedClasspath = ClassPath( exportedJars )
  override def targetClasspath = exportedClasspath
  import scala.collection.JavaConversions._

  private def resolve(suffix: String, hash: Option[String], useClassifier: Boolean): File = {
    logger.resolver(lib.blue("Resolving ")+this)
    val file = mavenCache ++ basePath(useClassifier) ++ "." ++ suffix
    val urls = repositories.map(_ ++ basePath(useClassifier) ++ "." ++ suffix)
    urls.find(
      lib.download(_, file, hash)
    ).getOrElse(
      throw new Exception(s"\nCannot resolve\n$this\nCan't find any of\n"++urls.mkString("\n"))
    )
    file
  }

  private def resolveHash(suffix: String, useClassifier: Boolean) = {
    Files.readAllLines(
      resolve( suffix ++ ".sha1", None, useClassifier ).toPath,
      StandardCharsets.UTF_8
    ).mkString("\n").split(" ").head.trim
  }

  def jarSha1: String = taskCache[BoundMavenDependency]("jarSha1").memoize{ resolveHash("jar", true) }
  def pomSha1: String = taskCache[BoundMavenDependency]("pomSha1").memoize{ resolveHash("pom", false) }
  def jar: File = taskCache[BoundMavenDependency]("jar").memoize{ resolve("jar", Some(jarSha1), true) }
  def pom: File = taskCache[BoundMavenDependency]("pom").memoize{ resolve("pom", Some(pomSha1), false) }

  private def pomXml = {
    logger.resolver( "Loading pom file: " ++ pom.string )
    XML.loadFile(pom.string)
  }
  // ========== pom traversal ==========

  private lazy val transitivePom: Seq[BoundMavenDependency] = {
    (pomXml \ "parent").collect{
      case parent =>
        BoundMavenDependency(
          cbtLastModified: Long,
          mavenCache,
          MavenDependency(
            (parent \ "groupId").text,
            (parent \ "artifactId").text,
            (parent \ "version").text
          ),
          repositories
        )
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
        cbtLastModified, mavenCache ++ basePath(true) ++ ".pom.dependencies"
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
        BoundMavenDependency( cbtLastModified, mavenCache, _, repositories )
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
