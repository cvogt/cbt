import cbt._

class Build(val context: Context) extends BaseBuild with Scalapb {
  override def compile = taskCache[Build]("compile").memoize{
    scalapb.apply
    super.compile
  }
}
