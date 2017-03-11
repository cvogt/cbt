import cbt._

class Build(context: cbt.Context) extends BasicBuild(context){
  override def dependencies = (
    super.dependencies
    ++
    Seq(
      GitDependency("https://github.com/cvogt/cbt.git", "f11b8318b85f16843d8cfa0743f64c1576614ad6", Some("test/library-test"))
    )
  )
}
