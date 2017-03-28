package cbt
// CLI interop
case class ExitCode( integer: Int ) extends interfaces.ExitCode {
  def ||( other: => ExitCode ) = if ( this == ExitCode.Success ) this else other
  def &&( other: => ExitCode ) = if ( this != ExitCode.Success ) this else other
}
object ExitCode {
  val Success = ExitCode( 0 )
  val Failure = ExitCode( 1 )
}

