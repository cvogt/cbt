import cbt._

class Build(val context: Context) extends BuildBuild with UberJar {

  override def projectName: String = "uber-jar-example"

  override def dependencies = super.dependencies ++
    Resolver( mavenCentral ).bind(
      ScalaDependency("com.chuusai", "shapeless", "2.3.1"),
      ScalaDependency("com.lihaoyi", "fansi", "0.1.3"),
      ScalaDependency("org.typelevel", "cats", "0.6.0")
    )

  override def uberJarName = projectName + "-0.0.1"

}
