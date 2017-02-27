import cbt._

class Build(val context: Context) extends UberJar {
  override def name: String = "uber-jar-example"
  def groupId: String = "com.example"
  def version = "0.0.1"

  override def dependencies = super.dependencies ++
    Resolver( mavenCentral ).bind(
      ScalaDependency("com.chuusai", "shapeless", "2.3.1"),
      ScalaDependency("com.lihaoyi", "fansi", "0.1.3"),
      ScalaDependency("org.typelevel", "cats", "0.6.0")
    )
}
