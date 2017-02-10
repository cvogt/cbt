package cbt
import org.scalatest._


trait ScalaTest extends BaseBuild{
  override def run: ExitCode = {
    import ScalaTestLib._
    val _classLoader = classLoader(context.classLoaderCache)
    val suiteNames = compileFile.map( d => discoverSuites(d, _classLoader) ).toVector.flatten
    runSuites( suiteNames.map( loadSuite( _, _classLoader ) ) )
    ExitCode.Success
  }
  override def dependencies = super.dependencies ++ Resolver( mavenCentral ).bind( ScalaDependency("org.scalatest","scalatest","2.2.4") )
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

