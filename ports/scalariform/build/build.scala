package migration_manager_build
import cbt._
class Build(val context: Context) extends AdvancedScala{ outer =>
  override def dependencies =
    Resolver( mavenCentral ).bind(
      ScalaDependency( "org.scala-lang.modules", "scala-xml", "1.0.6" ),
      ScalaDependency( "org.scala-lang.modules", "scala-parser-combinators", "1.0.5" )
    )

  val scalariform = GitDependency.checkout(
    "https://github.com/scala-ide/scalariform.git", "f53978c60579fa834ac9e56986a6133c0a621bfd"
  )

  override def sources = generatedSources
  override def generatedSources = Seq(
    scalariform / "scalariform" / "src" / "main" / "scala" / "scalariform"
  )

  /*
  // currently does not compile, scalatest changed too much in 3.0.1
  override def test = new BasicBuild( context ) with ScalaTest{
    override def projectDirectory = {
      val d = outer.projectDirectory / "test"
      d.mkdirs
      d
    }
    override def dependencies = Seq( outer ) ++
      Resolver( mavenCentral ).bind(
        ScalaDependency( "org.scalatest", "scalatest", "3.0.1" )
      )
    override def sources = generatedSources
    override def generatedSources = Seq(
      scalariform / "scalariform" / "src" / "test" / "scala" / "scalariform"
    )
  }
  */
}
