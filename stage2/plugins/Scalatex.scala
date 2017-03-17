package cbt
import java.io.File
import java.nio.file.Files
trait Scalatex extends BaseBuild {
  override def dependencies = super.dependencies :+ libraries.cbt.scalatex

  def scalatex = (
    Scalatex.apply(
      lib,
      Math.max( context.cbtLastModified, ( dependencies ++ context.parentBuild ).map( _.lastModified ).max )
    )
    .config(
      lib.autoRelative(
        lib.sourceFiles( sources, _.string endsWith ".scalatex" )
          ++ projectDirectory.listFiles.toVector.filter( _.toString.endsWith( ".scalatex" ) )
      ).map {
          case ( file, name ) => file -> name.stripPrefix( File.separator ).stripSuffix( ".scalatex" )
        },
      lib.autoRelative( Seq( projectDirectory / "static" ) ),
      target / "html",
      projectDirectory / "src_generated" / "scalatex",
      dependencyClasspath
    )
  )
}
/** @param htmlTarget directory where the html files are generated
  */
object Scalatex {
  case class apply(
    lib: Lib, parentsLastModified: Long
  )(
    implicit
    logger: Logger, transientCache: java.util.Map[AnyRef, AnyRef], classLoaderCache: cbt.ClassLoaderCache
  ) {
    case class config(
      scalatexFiles:       Seq[( File, String )],
      staticFiles:         Seq[( File, String )],
      htmlTarget:          File,
      srcGenerated:        File,
      dependencyClasspath: ClassPath,
      transform:           String                = "cbt.plugins.scalatex.runtime.layout", // function String => String
      linkStaticFile:      Boolean               = true,
      `package`:           String                = "cbt.plugins.scalatex.runtime"
    ) {
      def apply = {
        staticFiles.map {
          case ( file, relative ) => file -> htmlTarget / relative
        }.collect {
          case ( from, to ) if from.isDirectory                => to.mkdirs
          case ( from, to ) if !from.isDirectory && !to.exists => Files.createLink( to.toPath, from.toPath )
        }
        lib.cached(
          srcGenerated,
          ( parentsLastModified +: scalatexFiles.map( _._1 ).map( _.lastModified ) ).max
        ) { () =>
          lib.writeIfChanged(
            srcGenerated / "ScalatexGenerated.scala",
            s"""// Last generated at ${java.time.ZonedDateTime.now}
package ${`package`}
object ScalatexGenerate extends cbt.plugins.scalatex.runtime.AbstractMain{
  def dependencyClasspath: Seq[java.io.File] = Seq(
    ${dependencyClasspath.files.map( _.quote ).mkString( ",\n    " )}
  )
  def htmlTarget = ${htmlTarget.quote}
  import scalatags.Text.all._
  def files = Seq[(String, scalatags.Text.all.SeqFrag[scalatags.Text.all.Frag])](
    ${scalatexFiles.map { case ( from, to ) => s"${to.quote} -> scalatex.twf(${from.string.quote})" }.mkString( ",\n    " )}
  )
  def render: scalatags.Text.all.SeqFrag[scalatags.Text.all.Frag] => String => String = $transform
}
""".trim ++ "\n"
          )
        }
      }
    }
  }
}
