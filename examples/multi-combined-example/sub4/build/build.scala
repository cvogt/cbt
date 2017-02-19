package cbt_build.cbt_examples.multi_combined_example.sub4
import cbt._
class Build(val context: Context) extends BaseBuild{
  def sub41 = new Sub41(context)
}
class Sub41(val context: Context) extends BaseBuild{
  override def projectDirectory = context.workingDirectory / "sub41"
  def sub42 = new Sub42(context.copy(workingDirectory=projectDirectory))
}
class Sub42(val context: Context) extends BaseBuild{
  override def projectDirectory = context.workingDirectory / "sub42"
  def hello = "Hello from Sub42"
}
