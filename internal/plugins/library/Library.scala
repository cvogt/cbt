package cbt_internal
import cbt._
import java.io._
import scala.concurrent._
import scala.concurrent.duration._
trait Library extends Scalariform with GoogleJavaFormat with DynamicOverrides with AdvancedScala with ScalaXRay{
  def inceptionYear: Int
  def description: String
  def version = ???
  override def compile = {
    googleJavaFormat()
    scalariform()
    super.compile
  }

  def publishIfChanged = newBuild[PublishIfChanged]({s"""
    def inceptionYear = $inceptionYear
    def description = ${description.quote}
    def apply = if(changedInMaster) publish
  """})
}

trait PublishIfChanged extends PackageJars with DynamicOverrides with Shared{
  override def url = super.url ++ "/libraries/" ++ name

  def gitHash = {
    val p = new ProcessBuilder(
        "git rev-parse HEAD".split(" "): _*
    )
    .directory( projectDirectory )
    .start

    val sout = new InputStreamReader(p.getInputStream);
    import scala.concurrent.ExecutionContext.Implicits.global
    val out = Future(blocking(Iterator.continually(sout.read).takeWhile(_ != -1).map(_.toChar).mkString))
    p.waitFor
    val revision = Await.result( out, Duration.Inf ).trim
    revision
  }
  override def version = "rev-"++gitHash

  def changedInMaster = (
    0 ===
    new ProcessBuilder(
        "git diff --exit-code --quiet master..master^ .".split(" "): _*
    )
    .directory( projectDirectory )
    .start
    .waitFor
  )
}
