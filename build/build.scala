import cbt._
import java.net.URL
import java.io.File
import scala.collection.immutable.Seq

import tut.TutMain

class Build(context: Context) extends BasicBuild(context){
  // FIXME: somehow consolidate this with cbt's own boot-strapping from source.
  override def dependencies = super.dependencies :+ MavenRepository.central.resolve(
    MavenDependency("net.incongru.watchservice","barbary-watchservice","1.0"),
    MavenDependency("org.eclipse.jgit", "org.eclipse.jgit", "4.2.0.201601211800-r"),
    MavenDependency("com.typesafe.zinc","zinc","0.3.9"),
    ScalaDependency("org.scala-lang.modules","scala-xml","1.0.5")
  )
  override def sources = Seq(
    "nailgun_launcher", "stage1", "stage2"
  ).map(d => projectDirectory ++ ("/" + d))

  private object tutCache extends Cache[Unit]
  def tut = {

    // tut is normally run as a commandline app so it wants strings :-\
    // we can modify tut to add a typed runner and provide a logging hook
    // but as a proof of concept yep it works
    val in  = new File(projectDirectory, "doc/src/tut")
    val out = new File(projectDirectory, "doc/target/tut")
    val re  = """.*\.(md|txt|htm|html)"""
    val cp  = dependencyClasspath.string // we really want cbt on here too
    val opt = "-cp" :: cp :: scalacOptions.toList
    TutMain.runl(in.getAbsolutePath :: out.getAbsolutePath :: re :: opt).unsafePerformIO

  }

}