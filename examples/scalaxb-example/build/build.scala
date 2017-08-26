import cbt._

class Build(val context: Context) extends BaseBuild with ScalaXB {
  override def compile = taskCache[Build]("compile").memoize{
    generateScalaXBSources().defaultConfig.apply
    super.compile
  }
}
