package cbt
/**
  * A scala compiler plugin for more concise errors
  * See: https://github.com/tek/splain
  */
trait Splain extends BaseBuild{
  def splain = Splain.apply(
    scalaMajorVersion, context.cbtLastModified, context.paths.mavenCache
  ).config()

  override def scalacOptions = super.scalacOptions ++ splain.scalacOptions
}

object Splain{
  abstract class SplainOption( name: String, value: String ){
    def scalacOption = "-P:splain:" ~ name ~ ":" ~ value
  }

  case class all( value: Boolean = true ) extends SplainOption( "all", value.toString )
  case class infix( value: Boolean = true ) extends SplainOption( "infix", value.toString )
  case class foundreq( value: Boolean = true ) extends SplainOption( "foundreq", value.toString )
  case class implicits( value: Boolean = true ) extends SplainOption( "implicits", value.toString )
  case class bounds( value: Boolean = false ) extends SplainOption( "bounds", if( value ) "on" else "off" )
  case class color( value: Boolean = true ) extends SplainOption( "color", value.toString )
  case class breakinfix( value: Int = 0 ) extends SplainOption( "breakinfix", value.toString )
  case class tree( value: Boolean = true ) extends SplainOption( "tree", value.toString )
  case class compact( value: Boolean = false ) extends SplainOption( "compact", if( value ) "on" else "off" )
  case class boundsimplicits( value: Boolean = true ) extends SplainOption( "boundsimplicits", value.toString )
  case class truncrefined( value: Int = 0 ) extends SplainOption( "truncrefined", value.toString )

  case class apply(
    scalaMajorVersion: String, cbtLastModified: Long, mavenCache: java.io.File
  )(
    implicit logger: Logger, transientCache: java.util.Map[AnyRef,AnyRef], classLoaderCache: ClassLoaderCache
  ){
    case class config( version: String = "0.2.4", options: Seq[SplainOption] = Seq( all() ) ){
      def dependency = MavenResolver( cbtLastModified, mavenCache, mavenCentral ).bindOne(
        MavenDependency( "io.tryp", "splain_"+scalaMajorVersion, version )
      )
      def scalacOptions = ( "-Xplugin:" ~ dependency.jar.string ) +: options.map( _.scalacOption )
    }
  }
}
