import cbt._
class Build(val context: Context) extends DynamicOverrides with CommandLineOverrides{
  def foo2 = "Build"
  def bar2: String =
    newBuild[Build]{"""
      override def foo2 = "Bar2: "+Option(getClass.getName)
    """}.foo2

  def baz2: String =
    newBuild[Build]{"""
      override def foo2 = "Baz2: "+Option(getClass.getName)
      override def baz2 = bar2
    """}.baz2
  def foo = "Build"

  def bar: String = newBuild[Bar].bar
  def baz: String = newBuild[Baz].baz
  def bam: String = newBuild[Bam].baz
}
trait Bar extends Build{
  override def bar: String = foo
  override def foo = "Bar: "+getClass.getName
}
trait Baz extends Build{
  override def foo = "Baz: "+getClass.getName
  override def baz = bar
}
trait Bam extends Bar{
  override def foo = "Baz: "+getClass.getName
  override def baz = bar
}
