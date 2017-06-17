import cbt._
class Build(val context: Context) extends PackageJars{
  outer =>
  def groupId = "org.cvogt"
  def version = "0.9-SNAPSHOT"
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
