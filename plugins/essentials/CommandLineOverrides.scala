package cbt
trait CommandLineOverrides extends DynamicOverrides{
  def `with`: Any = {
    lib.callReflective(
      newBuild[DynamicOverrides](
        context.copy(
          args = context.args.drop(2)
        )
      )( s"""
        ${context.args.lift(0).getOrElse("")}
      """ ),
      context.args.lift(1) orElse Some("void")
    )
  }
  def eval = {
    lib.callReflective(
      newBuild[CommandLineOverrides](
        context.copy(
          args = ( context.args.lift(0).map("println{ "+_+" }") ).toSeq
        )
      ){""},
      Some("with")
    )
  }
}
