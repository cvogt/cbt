package cbt.scalatest

import org.scalatest._

import java.io.File

object Runner {
  def run( classpath: Array[File], classLoader: ClassLoader ): Unit = {
    val suiteNames = classpath.map( d => discoverSuites( d, classLoader ) ).flatten
    runSuites( suiteNames.map( loadSuite( _, classLoader ) ) )
  }

  def runSuites( suites: Seq[Suite] ) = {
    def color: Boolean = true
    def durations: Boolean = true
    def shortstacks: Boolean = true
    def fullstacks: Boolean = true
    def stats: Boolean = true
    def testName: String = null
    def configMap: ConfigMap = ConfigMap.empty
    suites.foreach {
      _.execute( testName, configMap, color, durations, shortstacks, fullstacks, stats )
    }
  }

  def discoverSuites( discoveryPath: File, classLoader: ClassLoader ): Seq[String] = {
    classLoader
      .loadClass( "org.scalatest.tools.SuiteDiscoveryHelper" )
      .getMethod( "discoverSuiteNames", classOf[List[_]], classOf[ClassLoader], classOf[Option[_]] )
      .invoke( null, List( discoveryPath.toString ++ "/" ), classLoader, None )
      .asInstanceOf[Set[String]]
      .toVector
  }
  def loadSuite( name: String, classLoader: ClassLoader ) = {
    classLoader.loadClass( name ).getConstructor().newInstance().asInstanceOf[Suite]
  }
}
