package cbt
trait Plugin extends BaseBuild{
  override def dependencies =
    super.dependencies :+ context.cbtDependency :+ DirectoryDependency( context.cbtHome ++ "/plugins/essentials" )
}
