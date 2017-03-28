package cbt.reflect
import java.lang.reflect.Method
case class StaticMethod[Arg, Result]( function: Arg => Result, method: Method ) extends ( Arg => Result ) {
  def apply( arg: Arg ): Result = function( arg )
}
