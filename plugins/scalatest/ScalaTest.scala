import cbt._
import java.net.URL
import java.io.File
import scala.collection.immutable.Seq
import org.scalatest._
import org.scalatest

/* FIXME:
 - Separate out SbtLayout
 - Allow depending on this via a git dependency.
   Probably by adding support for subfolders to "GitDependency"
*/

trait SbtLayout extends BasicBuild{
  outer =>
  override def sources = Seq( projectDirectory ++ "/src/main/scala" )
  def testSources = projectDirectory ++ "/src/test/scala"
  def testDependencies: Seq[Dependency] = Nil
  lazy val testBuild = 
    new BasicBuild(context.copy(projectDirectory = testSources)) with ScalaTest{
      override def target = outer.target
      override def compileTarget = outer.scalaTarget ++ "/test-classes"
      override def dependencies = (outer +: testDependencies) ++ super.dependencies 
    }
  override def test: Option[ExitCode] =
    if(testSources.exists) Some( testBuild.run )
    else None
}

trait ScalaTest extends BasicBuild{
  override def run: ExitCode = {
    import ScalaTestLib._
    val _classLoader = classLoader(context.classLoaderCache)
    val suiteNames = compile.map( d => discoverSuites(d, _classLoader) ).toVector.flatten
    runSuites( suiteNames.map( loadSuite( _, _classLoader ) ) )
    ExitCode.Success
  }
}

object ScalaTestLib{
  def runSuites(suites: Seq[Suite]) = {
    def color: Boolean = true
    def durations: Boolean = true
    def shortstacks: Boolean = true
    def fullstacks: Boolean = true
    def stats: Boolean = true
    def testName: String = null
    def configMap: ConfigMap = ConfigMap.empty
    suites.foreach{
      _.execute(testName, configMap, color, durations, shortstacks, fullstacks, stats)
    }
  }

  def discoverSuites(discoveryPath: File, _classLoader: ClassLoader): Seq[String] = {
    _classLoader
      .loadClass("org.scalatest.tools.SuiteDiscoveryHelper")
      .getMethod("discoverSuiteNames", classOf[List[_]], classOf[ClassLoader], classOf[Option[_]])
      .invoke(null, List(discoveryPath.string ++ "/"), _classLoader, None)
      .asInstanceOf[Set[String]]
      .to
  }
  def loadSuite(name: String, _classLoader: ClassLoader) = {
    _classLoader.loadClass(name).getConstructor().newInstance().asInstanceOf[Suite]
  }  
}

