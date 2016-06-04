import cbt._
class Build( context: Context ) extends BuildBuild( context ){

  override def dependencies = (
    super.dependencies ++
      Resolver( mavenCentral ).bind(
      ScalaDependency("org.scalatest","scalatest","2.2.4")
    ) :+ BuildDependency(new File(context.cbtHome + "/plugins/scalatest"))
  )
}
