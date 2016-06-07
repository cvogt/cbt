import cbt._
class Build(val context: Context) extends SbtLayout {

  override def dependencies = (
    super.dependencies ++
      Resolver( mavenCentral ).bind(
        ScalaDependency("org.scalatest","scalatest","2.2.4")
      )
  )
}