package cbt
trait AdvancedScala extends BaseBuild{
  override def scalacOptions = super.scalacOptions ++ Seq(
    "-language:postfixOps",
    "-language:implicitConversions",
    "-language:higherKinds",
    "-language:existentials",
    "-language:experimental.macros"
  )
}
