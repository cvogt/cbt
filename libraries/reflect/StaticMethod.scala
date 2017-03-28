package cbt.reflect
case class StaticMethod[Arg, Result]( function: Arg => Result, name: String ) extends ( Arg => Result ) {
  def apply( arg: Arg ): Result = function( arg )
}
