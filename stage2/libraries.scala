package cbt
class libraries( context: Context, scalaVersion: String, scalaMajorVersion: String ) {
  private def dep( name: String ) = DirectoryDependency(
    context.copy(
      scalaVersion     = Some( scalaVersion ),
      workingDirectory = context.cbtHome / "libraries" / name
    ),
    None
  )
  def captureArgs = dep( "capture_args" )
  def eval = dep( "eval" )
  def file = dep( "file" )
  def proguard = dep( "proguard" )
  def reflect = dep( "reflect" )
  def common_0 = dep( "common-0" )
  def common_1 = dep( "common-1" )
  def interfaces = dep( "interfaces" )
  def scalatestRunner = dep( "scalatest-runner" )
}
