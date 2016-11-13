import cbt._
import cbt._
trait SharedCbtBuild extends BaseBuild{
  override def defaultScalaVersion = "2.10.6"
}


class Shared(val context: Context) extends SharedCbtBuild

class Sub(val context:Context) extends SharedCbtBuild{
  override def dependencies = Seq(new Shared(
    context.copy(
      projectDirectory = projectDirectory ++ "/../shared"
    )
  ))
}

class Build(val context: Context) extends BaseBuild{
  /*
  Currently each sub build nested into the main build needs to be an instance
  of a top-level class taking a Context as the sole parameter, similar to the
  Build class itself. This restriction may be lifted for more flexibility at
  some point, see https://github.com/cvogt/cbt/issues/306
  */
  def sub1 = new Sub(
    context.copy(
      projectDirectory = projectDirectory ++ "/sub1"
    )
  )
  def sub2 = new Sub(
    context.copy(
      projectDirectory = projectDirectory ++ "/sub2"
    )
  )

  def sub3 = // DON'T DO THIS, anonymous classes are currently not supported here.
    new SharedCbtBuild{
      def context = Build.this.context.copy(
        projectDirectory = Build.this.projectDirectory ++ "/sub3"
      )
    }

  override def dependencies = Seq( sub1, sub2 ) // assembles all projects
}
