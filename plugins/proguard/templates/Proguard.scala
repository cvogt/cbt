package cbt
import java.io.File
import java.nio.file.Files.deleteIfExists

sealed class KeepOptionModifier(val string: String)
object KeepOptionModifier{
/* ${generated-top-level} */
}

trait Proguard extends BaseBuild {
  def proguard( keep: (Seq[KeepOptionModifier], String) ) = {
    ProguardLib(context.cbtLastModified, context.paths.mavenCache).proguard(
      outjars = Seq( scalaTarget / "proguarded.jar" ),
      injars = classpath,
      libraryjars = Proguard.`rt.jar`,
      keep = keep
    )
  }
}

object Proguard{
  val version = "5.3.2"
  val `rt.jar` = ClassPath( Seq( new File(System.getProperty("java.home"),"lib/rt.jar") ) )
}

case class ProguardLib(
  cbtLastModified: Long, mavenCache: File,
  dependency: Option[DependencyImplementation] = None
)(
  implicit logger: Logger, transientCache: java.util.Map[AnyRef,AnyRef], classLoaderCache: ClassLoaderCache
){
  /**
  Typed interface on top of the proguard command line tool.
  Check the official ProGuard docs for usage.
  Use `Some(None)` to call an option without arguments.
  Use `true` to set a flag.

  @see https://www.guardsquare.com/en/proguard/manual/refcard
  @see https://www.guardsquare.com/en/proguard/manual/usage

${generated-docs}
  */
  case class proguard(
/* ${generated-args} */
  ) extends ( () => ClassPath ){

    // type class rendering scala values into string arguments
    private class valueToStrings[T]( val apply: T => Option[Seq[String]] )
    private object valueToStrings{
      def apply[T:valueToStrings](value: T) = implicitly[valueToStrings[T]].apply(value)
      implicit object SeqFile extends valueToStrings[Seq[File]](v => Some(v.map(_.string)))
      implicit object ClassPath extends valueToStrings[ClassPath](v => Some(Seq(v.string)))
      implicit object File extends valueToStrings[File](v => Some(Seq(v.string)))
      implicit object String extends valueToStrings[String](v => Some(Seq(v)))
      implicit object Int extends valueToStrings[Int](i => Some(Seq(i.toString)))
      implicit object Boolean extends valueToStrings[Boolean]({
        case false => None
        case true => Some(Nil)
      })
      implicit def Option2[T:valueToStrings]: valueToStrings[Option[T]] = new valueToStrings(
        _.map(implicitly[valueToStrings[T]].apply(_).toSeq.flatten)
      )
      implicit def Option3[T:valueToStrings]: valueToStrings[Option[Option[String]]] = new valueToStrings(_.map(_.toSeq))
      implicit def SpecWithModifiers: valueToStrings[(Seq[KeepOptionModifier], String)] = new valueToStrings({
        case (modifiers, spec) => Some( Seq( modifiers.map(_.string).map(","++_).mkString ).filterNot(_ == "") :+ spec )
      })
    }

    // capture string argument values and names
    val capturedArgs = capture_args.captureArgs

    def apply: ClassPath = {
      val args = capturedArgs.args.map(arg => arg.copy(name="-"++arg.name)).flatMap(_.toSeqOption).flatten
      outjars.map(_.toPath).map(deleteIfExists)
      val c = dependency getOrElse MavenResolver( cbtLastModified, mavenCache, mavenCentral ).bindOne(
        MavenDependency("net.sf.proguard","proguard-base",Proguard.version)
      ) runMain (
        "proguard.ProGuard",
        args : _*
      )
      if(c != ExitCode.Success) throw new Exception
      ClassPath(outjars)
    }
  }
}
