import cbt._
class Build(val context: Context) extends SbtLayoutMain {
  outer =>
  override def test: Option[ExitCode] = Some{
    new BasicBuild(context) with ScalaTest with SbtLayoutTest{
      override def dependencies = outer +: super.dependencies 
    }.run
  }  
}
