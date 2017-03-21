package cbt_build.cbt
import cbt._
import cbt_internal._

class Build(val context: Context) extends Shared with Scalariform with PublishLocal{
  override def name: String = "cbt"
  override def version: String = "0.1"
  override def description: String = "Fast, intuitive Build Tool for Scala"
  override def inceptionYear: Int = 2015

  // FIXME: somehow consolidate this with cbt's own boot-strapping from source.
  override def dependencies = {
    super.dependencies ++ Resolver(mavenCentral).bind(
      MavenDependency("org.eclipse.jgit", "org.eclipse.jgit", "4.2.0.201601211800-r"),
      ScalaDependency("org.scala-lang.modules","scala-xml",constants.scalaXmlVersion)
    ) :+ libraries.reflect :+ libraries.eval
  }

  override def sources = Seq(
    "nailgun_launcher", "stage1", "stage2", "compatibility"
  ).map( projectDirectory / _ ).flatMap( _.listOrFail )

  override def scalariform = super.scalariform.copy(
    Seq(
      context.cbtHome / "stage2" / "DirectoryDependency.scala",
      context.cbtHome / "stage2" / "LazyDependency.scala",
      context.cbtHome / "stage2" / "libraries.scala",
      context.cbtHome / "stage2" / "plugins.scala"
    )
  )

  override def compile = {
    scalariform()
    super.compile
  }
}
