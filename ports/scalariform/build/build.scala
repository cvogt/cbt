package migration_manager_build
import cbt._
class Build(val context: Context) extends AdvancedScala{ outer =>
  override def dependencies = Seq( libraries.scala.xml, libraries.scala.parserCombinators )

  val scalariform = GitDependency.checkout(
    "https://github.com/scala-ide/scalariform.git", "f53978c60579fa834ac9e56986a6133c0a621bfd"
  )

  override def sources = Seq(
    scalariform / "scalariform" / "src" / "main" / "scala" / "scalariform"
  )

  override def scalacOptions = super.scalacOptions ++ Seq(
    "-language:implicitConversions", "-language:reflectiveCalls"
  )

  override def test = new BasicBuild( context ) with ScalaTest{
    override def target = outer.target / "test"
    override def dependencies = super.dependencies :+ outer
    override def scalacOptions = outer.scalacOptions
    override def sources = Seq(
      scalariform / "scalariform" / "src" / "test" / "scala" / "scalariform"
    )
  }
}
