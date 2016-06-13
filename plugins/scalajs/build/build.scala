import cbt._

class Build(context: Context) extends BasicBuild(context) {

  override def dependencies =
    super.dependencies :+
      context.cbtDependency
}

