package proguard_build.build
import cbt._
class Build(val context: Context) extends BuildBuild with CbtInternal{
  override def dependencies = (
    super.dependencies ++ // don't forget super.dependencies here for scala-library, etc.
    Resolver( mavenCentral, sonatypeReleases ).bind(
      ScalaDependency("org.scala-lang.modules","scala-xml","1.0.5"),
       "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1"
    ) ++ Seq( cbtInternal.library )
  )
}
