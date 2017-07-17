import java.io.File

import cbt._
import giter8._
import scala.util._

class Build(val context: Context) extends BaseBuild {
  def createTemplate = {
    val helper = new JgitHelper(new Git(new JGitInteractor), G8TemplateRenderer)
    (context.args.toList match {
      case t :: Nil => Right(t, context.cwd, Seq.empty)
      case t :: x :: Nil if x.startsWith("--") => Right(t, context.cwd, Seq(x))
      case t :: x :: xs => Right(t, new File(x), xs)
      case Nil => Left("Please, specify a template name")
    })
    .right.flatMap { case (template, dir, args) =>
      Either.cond(args.forall(validateArgument),
        (template, dir, args),
        """Incorrect arguments, it should be --<name>=<value>
          |Example --name=my_cool_project
        """.stripMargin)
    }
    .right.flatMap { case (template, dir, args) =>
      val config = Config(template)
      helper.run(config, args, dir)
    }
    .fold(f => Failure(new Exception(f)), s => Success(s))
    .get
  }

  private def validateArgument(arg: String) =
    arg.matches("""--[\w]+=[\w]+""")
}
