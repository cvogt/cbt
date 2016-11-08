import cbt._

// cbt:https://github.com/cvogt/cbt.git#0fcdda6062c4e2720f602d976d85961970f44fa0
class Build(val context: cbt.Context) extends PackageJars{
  override def dependencies = super.dependencies ++ Seq(
    DirectoryDependency( context.cbtHome ++ "/test/library-test" )
  ) ++ Resolver( mavenCentral ).bind(
    MavenDependency("org.eclipse.jgit", "org.eclipse.jgit", "4.2.0.201601211800-r"),
    MavenDependency("com.spotify", "missinglink-core", "0.1.1")
  )
  def groupId: String = "cbt.test"
  def defaultVersion: String = "0.1"
  def name: String = "simple-fixed-cbt"
}