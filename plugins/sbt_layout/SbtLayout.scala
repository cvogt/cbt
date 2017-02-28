package cbt
// TODO: move this into stage2 to avoid having to call zinc separately for this as a plugin
trait SbtLayoutTest extends BaseBuild{
  override def sources = Seq(projectDirectory ++ "/src/test/scala")
  override def compileTarget = super.compileTarget.getParentFile ++ "/test-classes"
}

trait SbtLayoutMain extends BaseBuild{
  override def sources = Seq( projectDirectory ++ "/src/main/scala" )
}
