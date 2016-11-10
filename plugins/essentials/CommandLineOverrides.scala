package cbt
trait CommandLineOverrides extends DynamicOverrides{
  def `with`: Any = {
    new lib.ReflectObject(
      newBuild[DynamicOverrides](
        context.copy(
          args = context.args.drop(2)
        )
      )( s"""
        ${context.args.lift(0).getOrElse("")}
      """ )
    ){
      def usage = ""
    }.callNullary(context.args.lift(1) orElse Some("void"))
  }
  def eval = {
    new lib.ReflectObject(
      newBuild[CommandLineOverrides](
        context.copy(
          args = ( context.args.lift(0).map("println{ "+_+" }") ).toSeq
        )
      ){""}
    ){def usage = ""}.callNullary(Some("with"))
  }
}
