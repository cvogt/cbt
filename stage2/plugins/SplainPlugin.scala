package cbt
import java.io.File

/**
  * A scala compiler plugin for more concise errors
  * See: https://github.com/tek/splain
  */
trait SplainPlugin extends BaseBuild{

  protected[this] type On = SplainPlugin.On.type
  protected[this] type Off = SplainPlugin.Off.type
  protected[this] type All = SplainPlugin.All
  protected[this] type Infix = SplainPlugin.Infix
  protected[this] type FoundReq = SplainPlugin.FoundReq
  protected[this] type Implicits = SplainPlugin.Implicits
  protected[this] type Bounds = SplainPlugin.Bounds
  protected[this] type Color = SplainPlugin.Color
  protected[this] type BreakInfix = SplainPlugin.BreakInfix
  protected[this] type Tree = SplainPlugin.Tree
  protected[this] type Compact = SplainPlugin.Compact
  protected[this] type BoundsImplicits = SplainPlugin.BoundsImplicits
  protected[this] type TruncRefined = SplainPlugin.TruncRefined

  protected[this] val On = SplainPlugin.On
  protected[this] val Off = SplainPlugin.Off
  protected[this] val All = SplainPlugin.All
  protected[this] val Infix = SplainPlugin.Infix
  protected[this] val FoundReq = SplainPlugin.FoundReq
  protected[this] val Implicits = SplainPlugin.Implicits
  protected[this] val Bounds = SplainPlugin.Bounds
  protected[this] val Color = SplainPlugin.Color
  protected[this] val BreakInfix = SplainPlugin.BreakInfix
  protected[this] val Tree = SplainPlugin.Tree
  protected[this] val Compact = SplainPlugin.Compact
  protected[this] val BoundsImplicits = SplainPlugin.BoundsImplicits
  protected[this] val TruncRefined = SplainPlugin.TruncRefined

  def splainVersion = "0.2.4"

  def splainParams: Seq[SplainPlugin.SplainParam] = Seq( All() )

  override def scalacOptions =
    super.scalacOptions ++
    SplainPlugin.scalacOptions(
      SplainPlugin.dependencies( scalaMajorVersion, splainVersion, context.cbtLastModified, context.paths.mavenCache ).jar,
      splainParams
    )
}

object SplainPlugin{
  protected sealed abstract class SplainParam(name: String){
    type A
    def value: A
    def scalacString: String = s"-P:splain:$name:$value"
  }

  protected sealed abstract class Toggle(name: String){
    override def toString = name
  }

  case object On extends Toggle( "on" )
  case object Off extends Toggle( "off" )

  case class All( value: Boolean = true ) extends SplainParam( "all" ){ type A = Boolean }
  case class Infix( value: Boolean = true ) extends SplainParam( "infix" ){ type A = Boolean }
  case class FoundReq( value: Boolean = true ) extends SplainParam( "foundreq" ){ type A = Boolean }
  case class Implicits( value: Boolean = true ) extends SplainParam( "implicits" ){ type A = Boolean }
  case class Bounds( value: Toggle = Off ) extends SplainParam( "bounds" ){ type A = Toggle }
  case class Color( value: Boolean = true ) extends SplainParam( "color" ){ type A = Boolean }
  case class BreakInfix( value: Int = 0 ) extends SplainParam( "breakinfix" ){ type A = Int }
  case class Tree( value: Boolean = true ) extends SplainParam( "tree" ){ type A = Boolean }
  case class Compact( value: Toggle = Off ) extends SplainParam( "compact" ){ type A = Toggle }
  case class BoundsImplicits( value: Boolean ) extends SplainParam( "boundsimplicits" ){ type A = Boolean }
  case class TruncRefined( value: Int = 0 ) extends SplainParam( "truncrefined" ){ type A = Int }

  private def dependencies(
    scalaMajorVersion: String, splainVersion: String, cbtLastModified: Long, mavenCache: java.io.File
  )(
    implicit logger: Logger, transientCache: java.util.Map[AnyRef,AnyRef], classLoaderCache: ClassLoaderCache
  ) =
    MavenResolver( cbtLastModified, mavenCache, mavenCentral ).bindOne(
      MavenDependency( "io.tryp", "splain_"+scalaMajorVersion, splainVersion )
    )

  private def scalacOptions( jarPath: File, params: Seq[SplainParam] ) =
    Seq( "-Xplugin:" ++ jarPath.string ) ++ params.map(_.scalacString)
}
