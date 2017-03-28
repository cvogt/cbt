package migration_manager_build
import cbt._
class Build(val context: Context) extends BaseBuild{
  override def dependencies =
    Resolver( mavenCentral ).bind(
      MavenDependency( "org.scala-lang", "scala-compiler", constants.scalaVersion )
    )
  def mima = GitDependency.checkout(
    "git@github.com:typesafehub/migration-manager.git", "92cbce52b4bf04ca1c338f34818ebfb9f0ebc285"
  )
  override def generatedSources = Seq( mima / "core" )
}
