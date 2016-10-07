import cbt._

class Build(val context: Context) extends MetaBuild{
  override def dependencies =
    super.dependencies ++ // don't forget super.dependencies here
    Seq(    
      // source dependency
      DirectoryDependency( projectDirectory.getParentFile ++ "/shared-build" )
    )
}
