package cbt
import org.scalatest._


trait ScalaTest extends BaseBuild{
  override def run: ExitCode = {
    import ScalaTestLib._
    val suiteNames = exportedClasspath.files.map( d => discoverSuites(d, classLoader) ).flatten
    runSuites( suiteNames.map( loadSuite( _, classLoader ) ) )
    ExitCode.Success
  }
  override def dependencies = super.dependencies ++ Resolver( mavenCentral ).bind(
    ScalaDependency("org.scalatest","scalatest","3.0.1")
  )
}

object ScalaTestLib{
  import java.io.File
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

  def discoverSuites(discoveryPath: File, classLoader: ClassLoader): Seq[String] = {
    classLoader
      .loadClass("org.scalatest.tools.SuiteDiscoveryHelper")
      .getMethod("discoverSuiteNames", classOf[List[_]], classOf[ClassLoader], classOf[Option[_]])
      .invoke(null, List(discoveryPath.string ++ "/"), classLoader, None)
      .asInstanceOf[Set[String]]
      .toVector
  }
  def loadSuite(name: String, classLoader: ClassLoader) = {
    classLoader.loadClass(name).getConstructor().newInstance().asInstanceOf[Suite]
  }
}

