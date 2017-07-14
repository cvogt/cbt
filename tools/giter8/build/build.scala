import java.io.File

import cbt._
import giter8._

class Build(val context: Context) extends BaseBuild {
  def createTemplate: String = {
    val helper = new JgitHelper(new Git(new JGitInteractor), G8TemplateRenderer)
    val dir = new File(context.args.head)
    val config = Config("scala/scala-seed.g8")
    helper.run(config, Seq.empty, dir).toString
  }
}
