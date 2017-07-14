import java.io.File

import cbt._
import giter8._

class Build(val context: Context) extends BaseBuild {
  def createTemplate: String = {
    val helper = new JgitHelper(new Git(new JGitInteractor), G8TemplateRenderer)
    val (dir, args) = context.args.toList match {
      case Nil => (context.cwd, Seq.empty)
      case x::Nil if x.startsWith("--") => (context.cwd, Seq(x))
      case x :: xs => (new File(x), xs)
    }
    val config = Config("scala/scala-seed.g8")
    helper.run(config, args, dir).toString
  }
}
