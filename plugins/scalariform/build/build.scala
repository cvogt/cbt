import cbt._

class Build(val context: Context) extends Plugin {
  override def dependencies = super.dependencies :+ ports.scalariform
}
