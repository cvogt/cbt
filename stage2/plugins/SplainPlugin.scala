package cbt
import java.io.File

/**
  * A scala compiler plugin for more concise errors
  * See: https://github.com/tek/splain
  */
trait SplainPlugin extends BaseBuild{
  import SplainPlugin.ParameterPair

  def splainVersion = "0.2.4"

  def splainArguments: Seq[ParameterPair] = Seq( "all" -> None )

  override def scalacOptions =
    super.scalacOptions ++
    SplainPlugin.scalacOptions(
      SplainPlugin.dependencies( scalaMajorVersion, splainVersion, context.cbtLastModified, context.paths.mavenCache ).jar,
      splainArguments
    )
}

object SplainPlugin{
  type ParameterPair = ( String, Option[String] )

  def dependencies(
    scalaMajorVersion: String, splainVersion: String, cbtLastModified: Long, mavenCache: java.io.File
  )(
    implicit logger: Logger, transientCache: java.util.Map[AnyRef,AnyRef], classLoaderCache: ClassLoaderCache
  ) =
    MavenResolver( cbtLastModified, mavenCache, mavenCentral ).bindOne(
      MavenDependency( "io.tryp", "splain_"+scalaMajorVersion, splainVersion )
    )

  def scalacOptions( jarPath: File, params: Seq[ParameterPair] ) =
    Seq( "-Xplugin:" ++ jarPath.string ) ++ mkParamList( params )

  private def mkParamList(params: Seq[ParameterPair]) =
    params.foldLeft( Seq.empty[String] ) {
      case (paramList, (param, Some(value))) =>
        paramList :+ s"-P:splain:$param:$value"
      case (paramList, (param, None)) =>
        paramList :+ s"-P:splain:$param"
    }
}

