package cbt

trait MultipleScalaVersions extends DynamicOverrides{
  def scalaVersions: Seq[String] = Seq(scalaVersion, "2.10.6")
  def cross: Seq[MultipleScalaVersions] =
    scalaVersions.map{ v =>
      newBuild[MultipleScalaVersions](context.copy(scalaVersion = Some(v)))("")
    }
}
