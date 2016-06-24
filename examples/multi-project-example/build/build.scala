import cbt._
class Build(val context: Context) extends SharedCbtBuild{
  override def dependencies =
    super.dependencies ++ // don't forget super.dependencies here
    Seq(    
      // source dependency
      DirectoryDependency( projectDirectory ++ "/sub1" ),
      DirectoryDependency( projectDirectory ++ "/sub2" )
    )
}
