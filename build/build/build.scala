import cbt._
import java.net.URL
import java.io.File
import scala.collection.immutable.Seq

class Build(context: Context) extends BuildBuild(context){
  override def dependencies = super.dependencies ++ Seq(
    // The download currently fails, because CBT is hardcoded to maven central,
    // but tut-core is published on bintray. I will work on support for
    // alternative maven repositories next. Needs to happen before 1.0 anyways.
    // ScalaDependency("org.tpolecat", "tut-core", "0.4.2")
  )
}
