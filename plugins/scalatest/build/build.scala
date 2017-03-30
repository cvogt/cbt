import cbt._

class Build(val context: Context) extends BaseBuild{
  override def dependencies = (
    super.dependencies
    :+ context.cbtDependency
  ) ++ Resolver( mavenCentral ).bind(
    ScalaDependency("org.scalatest","scalatest","3.0.1")
  )
}
