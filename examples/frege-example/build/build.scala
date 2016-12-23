import cbt._

class Build(val context: Context) extends Frege {
  override def classifier = Some("jdk7")
  override def inline = false
  override def fregeSource = "1.7"
  override def fregeTarget = "1.7"
}
