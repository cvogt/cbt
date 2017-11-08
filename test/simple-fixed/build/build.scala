import cbt._

class Build(context: cbt.Context) extends BasicBuild(context){
  override def dependencies = (
    super.dependencies
    ++
    Seq(
      GitDependency("https://github.com/cvogt/cbt.git", "2ab3402e4899e722905a3a5a0825c5af38706303", Some("test/library-test"))
    )
  )
}
