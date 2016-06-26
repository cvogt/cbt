import cbt._
// cbt:https://github.com/cvogt/cbt.git#75c32537cd8f29f9d12db37bf06ad942806f0239
class Build(val context: Context) extends BuildBuild{
  override def dependencies =
    super.dependencies ++ // don't forget super.dependencies here
    Seq(    
      // source dependency
      DirectoryDependency( projectDirectory.getParentFile.getParentFile ++ "/shared-build" )
    )
}
