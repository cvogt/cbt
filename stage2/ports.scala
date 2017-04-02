package cbt
class ports( context: Context, scalaVersion: String ) {
  private def dep( name: String ) = DirectoryDependency(
    context.copy(
      scalaVersion     = Some( scalaVersion ),
      workingDirectory = context.cbtHome / "ports" / name
    ),
    None
  )
  def mima = dep( "migration-manager" )
}
