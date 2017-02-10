import cbt._

// cbt:https://github.com/cvogt/cbt.git#e8673866b79f7473391dcee26243eee80d5d3cb6
class Build(val context: cbt.Context) extends PackageJars{
  override def dependencies = super.dependencies ++ Seq(
    DirectoryDependency( context.cbtHome ++ "/test/library-test" )
  ) ++ Resolver( mavenCentral ).bind(
    MavenDependency("org.eclipse.jgit", "org.eclipse.jgit", "4.2.0.201601211800-r"),
    MavenDependency("com.spotify", "missinglink-core", "0.1.1")
  )
  def groupId: String = "cbt.test"
  def version: String = "0.1"
  def name: String = "simple-fixed-cbt"
}