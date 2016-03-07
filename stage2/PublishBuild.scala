package cbt
import java.io.File
import java.net.URL
import scala.collection.immutable.Seq

abstract class PublishBuild(context: Context) extends PackageBuild(context){
  def name = artifactId
  def description: String
  def url: URL
  def developers: Seq[Developer]
  def licenses: Seq[License]
  def scmUrl: String
  def scmConnection: String
  def pomExtra: Seq[scala.xml.Node] = Seq()

  // ========== package ==========

  /** put additional xml that should go into the POM file in here */
  def pom: File = lib.pom(
    groupId = groupId,
    artifactId = artifactId,
    version = version,
    name = name,
    description = description,
    url = url,
    developers = developers,
    licenses = licenses,
    scmUrl = scmUrl,
    scmConnection = scmConnection,
    dependencies = dependencies,
    pomExtra = pomExtra,
    jarTarget = jarTarget
  )

  // ========== publish ==========
  final protected def releaseFolder = s"/${groupId.replace(".","/")}/$artifactId/$version/"
  def snapshotUrl = new URL("https://oss.sonatype.org/content/repositories/snapshots")
  def releaseUrl = new URL("https://oss.sonatype.org/service/local/staging/deploy/maven2")
  def publishSnapshot: Unit = lib.publishSnapshot(sourceFiles, pom +: `package`, snapshotUrl ++ releaseFolder )
  def publishSigned: Unit = lib.publishSigned(sourceFiles, pom +: `package`, releaseUrl ++ releaseFolder )
}
