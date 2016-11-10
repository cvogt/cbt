import java.net.URL

import cbt._

class Build(val context: Context) extends SonatypeRelease {
  def groupId: String = "com.github.rockjam"
  def version: String = "0.0.15"
  def name: String = "cbt-sonatype"

  def description: String = "Plugin for CBT to release artifacts to sonatype OSS"
  def developers: Seq[Developer] = Seq(
    Developer(
      "rockjam",
      "Nikolay Tatarinov",
      "GMT+3",
      new URL("https://github.com/rockjam")
    )
  )
  def inceptionYear: Int = 2016
  def licenses: Seq[cbt.License] = Seq(License.Apache2)
  def organization: Option[cbt.Organization] = None
  def scmConnection: String = ""
  def scmUrl: String = "https://github.com/rockjam/cbt-sonatype.git"
  def url: java.net.URL = new URL("https://github.com/rockjam/cbt-sonatype")

  override def dependencies =
    super.dependencies ++
      Resolver( mavenCentral ).bind(
        ScalaDependency("com.chuusai", "shapeless", "2.3.2")
      )
}
