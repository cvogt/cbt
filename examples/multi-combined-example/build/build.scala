package cbt_build.cbt_examples.multi_combined_example
import cbt._

trait ScalaVersion extends BaseBuild{
  override def defaultScalaVersion = "2.10.6"
}

trait SharedBuild extends ScalaVersion{
  override def dependencies: Seq[Dependency] = Seq( new SharedDependency(context) )
  // Type-safe embedding of other build. Requires dependency in build/build/build.scala
  def sub4 = new cbt_build.cbt_examples.multi_combined_example.sub4.Build(
    // currently you'll have to provide the correct working directory for that build here
    context.copy( workingDirectory = projectDirectory / "sub4" )
  )
  def sub6 = DirectoryDependency(context.workingDirectory / "sub6")
}

class SharedDependency(val context: Context) extends ScalaVersion{
  override def projectDirectory = context.workingDirectory / "shared"
}

class Sub1(val context: Context) extends SharedBuild{
  override def projectDirectory = context.workingDirectory / "sub1"
}

class Sub2(val context: Context) extends SharedBuild{
  override def projectDirectory = context.workingDirectory / "sub2"
  override def dependencies = super.dependencies ++ Seq( sub6 )
}

class Sub3(val context: Context) extends SharedBuild{
  override def projectDirectory = context.workingDirectory / "sub3"
  override def dependencies = Seq(
    // Embed another sub build reflectively. Convenient for simple dependencies
    DirectoryDependency(context.workingDirectory / "sub4", "sub41.sub42")
  )
}

class Sub5(val context: Context) extends SharedBuild{
  override def projectDirectory = context.workingDirectory / "sub5"
  override def dependencies = Seq( sub4.sub41.sub42 )
}

class Build(val context: Context) extends SharedBuild{
  /*
  Currently each sub build nested into the main build needs to be an instance
  of a top-level class taking a Context as the sole parameter, similar to the
  Build class itself. This restriction may be lifted for more flexibility at
  some point, see https://github.com/cvogt/cbt/issues/306
  */
  def sub1 = new Sub1(context)
  def sub2 = new Sub2(context)
  def sub3 = new Sub3(context)
  def sub5 = new Sub3(context)

  def helloFromSub42 = sub4.sub41.sub42.hello

  /*
  // DON'T DO THIS, anonymous classes are currently not supported here.
  def sub3 =
    new SharedCbtBuild{
      def context = Build.this.context.copy(
        workingDirectory = Build.this.projectDirectory ++ "/sub3"
      )
    }
  */

  override def dependencies = Seq( sub1, sub2 ) // assembles all projects
}
