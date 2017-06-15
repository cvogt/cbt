package kindprojector_example_build

import cbt._

class Build(val context: Context) extends BaseBuild with KindProjectorPlugin {

  override def scalacOptions: Seq[String] =
    super.scalacOptions ++
    Seq("-language:higherKinds")

}
