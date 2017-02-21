import cbt._
class Build(val context: Context) extends BaseBuild{
  outer =>
  override def dependencies = super.dependencies :+
    new ScalaCompilerDependency( context.cbtLastModified, context.paths.mavenCache, scalaVersion )

  override def test: Dependency = {
    new BasicBuild(context.copy(workingDirectory = projectDirectory ++ "/test")) with ScalaTest{
      override def dependencies = super.dependencies ++ Seq(
        DirectoryDependency(projectDirectory++"/..")
      )
    }
  }
}
