import cbt._

class Build(val context: Context) extends BaseBuild with IntelliJ {
  override def name = "idea-plugin-example"
}
