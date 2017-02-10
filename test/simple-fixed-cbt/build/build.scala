import cbt._

// cbt:https://github.com/cvogt/cbt.git#f11b8318b85f16843d8cfa0743f64c1576614ad6
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