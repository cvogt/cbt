import cbt._
class Build(val context: Context) extends BaseBuild{
  outer =>
  override def dependencies = super.dependencies :+
    new ScalaCompilerDependency( context.cbtLastModified, context.paths.mavenCache, scalaVersion )

  override def test: Option[ExitCode] = Some{
    new BasicBuild(context.copy(projectDirectory = projectDirectory ++ "/test")) with ScalaTest{
      override def dependencies = super.dependencies ++ Seq(
        DirectoryDependency(projectDirectory++"/..")
      )
    }.run
  }  
}
