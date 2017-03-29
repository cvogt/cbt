package cbt
class ports( context: Context ) {
  private def dep( name: String ) = DirectoryDependency(
    context.copy( workingDirectory = context.cbtHome / "ports" / name ),
    None
  )
  def mima = dep( "migration-manager" )
}
