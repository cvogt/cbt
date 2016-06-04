import cbt._
class Build( context: Context ) extends BasicBuild( context ) with SbtLayout {

  override def dependencies = (
    super.dependencies ++
      Resolver( mavenCentral ).bind(
        ScalaDependency("org.scalatest","scalatest","2.2.4")
      )
  )
}