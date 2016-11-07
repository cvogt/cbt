import cbt._

// cbt:https://github.com/cvogt/cbt.git#bf4ea112fe668fb7e2e95a2baca4989b16384783
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