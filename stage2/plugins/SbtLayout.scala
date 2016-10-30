package cbt

trait SbtLayoutTest extends BaseBuild{
  override def sources = Seq(projectDirectory ++ "/src/test/scala")
  override def compileTarget = super.compileTarget.getParentFile ++ "/test-classes"
}

trait SbtLayoutMain extends BaseBuild{
  override def sources = Seq( projectDirectory ++ "/src/main/scala" )
}
