package cbt.common_1
import cbt.ExitCode
object `package` extends Module {
  implicit class CbtExitCodeOps( val exitCode: ExitCode ) extends AnyVal with ops.CbtExitCodeOps
  implicit class TypeInferenceSafeEquals[T]( val value: T ) extends AnyVal with ops.TypeInferenceSafeEquals[T]
  implicit class CbtBooleanOps( val condition: Boolean ) extends AnyVal with ops.CbtBooleanOps
  implicit class CbtStringOps( val string: String ) extends AnyVal with ops.CbtStringOps
}

package ops {
  trait CbtExitCodeOps extends Any {
    def exitCode: ExitCode
    def ||( other: => ExitCode ) = if ( exitCode == ExitCode.Success ) exitCode else other
    def &&( other: => ExitCode ) = if ( exitCode != ExitCode.Success ) exitCode else other
  }
  trait TypeInferenceSafeEquals[T] extends Any {
    def value: T
    /** if you don't manually upcast, this will catch comparing different types */
    def ===( other: T ) = value == other
    def =!=( other: T ) = value != other // =!= instead of !==, because it has better precedence
  }
  trait CbtBooleanOps extends Any {
    def condition: Boolean
    def option[T]( value: => T ): Option[T] = if ( condition ) Some( value ) else None
  }
  trait CbtStringOps extends Any {
    def string: String
    def escape = string.replace( "\\", "\\\\" ).replace( "\"", "\\\"" )
    def quote = s""""$escape""""
    def ~( right: String ): String = string + right
  }
}

trait Module
