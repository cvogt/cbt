import cbt._
class Build(val context: Context) extends BaseBuild{
  override def sources = super.sources ++ Seq(
    projectDirectory.getParentFile ++ "/shared"
  )
}
