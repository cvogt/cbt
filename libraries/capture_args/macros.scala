package cbt.capture_args
import scala.reflect._
import scala.reflect.macros.blackbox.Context

case class Argument( annotations: Seq[annotation.Annotation], name: String, values: Option[Seq[String]] ){
  def toSeqOption = values.map( name +: _ )
}
case class Signature( name: String, args: Seq[Argument] )
object `package`{
  def captureArgsImplementation(c: Context): c.Tree = {
    import c.universe._

    def literal( a: Any ) = Literal(Constant(a))
    def ident( name: String ) = Ident(TermName(name))

    def findOwnerRecursive(symbol: Symbol, predicate: Symbol => Boolean): Option[Symbol] = {
      Option(symbol).flatMap{
        s =>
        if(s == NoSymbol) None else if(predicate(s)) Some(s) else findOwnerRecursive(s.owner, predicate)
      }
    }

    val method: MethodSymbol = (
      findOwnerRecursive(c.internal.enclosingOwner, _.isMethod).map(_.asMethod)
      orElse
      findOwnerRecursive(c.internal.enclosingOwner, _.isClass).map(_.asClass.primaryConstructor.asMethod)
      getOrElse {
        c.error(
          c.enclosingPosition,
          "embed needs to be called in the body of a qualified method"
        )
        ???
      }
    )
    val name = literal(method.name.decodedName.toString)
    // Note: method.paramLists requires explicitly annotated result type
    val params = method.paramLists.flatten.map(_.asTerm)

    val args = params.map{ s =>
      val name = literal( s.name.decodedName.toString )
      val i = ident( s.name.toString )
      q"_root_.cbt.capture_args.Argument( _root_.scala.Seq( ..${s.annotations.map(_.tree)} ), $name, valueToStrings($i) )"
    }
    val tree = q"""
    _root_.cbt.capture_args.Signature( name = $name, args = Seq( ..$args ) )
    """
    tree
  }
  def captureArgs: Signature = macro captureArgsImplementation
}
