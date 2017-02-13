import cbt._
class Build(val context: Context) extends MultipleScalaVersions{
  override def scalaVersions = Seq("2.10.5","2.11.7")
}
