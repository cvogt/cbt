package cbt
import java.io.File
import java.net.URL
import java.nio.file.Files.readAllBytes
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
  override def copy(context: Context) = super.copy(context).asInstanceOf[PublishBuild]

  protected def sonatypeCredentials = {
    // FIXME: this should probably not use cbtHome, but some reference to the system's host cbt
    new String(readAllBytes((context.cbtHome ++ "/sonatype.login").toPath)).trim
  }

  def publishSnapshot: Unit = {
    val snapshotBuild = copy( context.copy(version = Some(version+"-SNAPSHOT")) )
    val files = snapshotBuild.pom +: snapshotBuild.`package`
    lib.publishSnapshot(
      sourceFiles, files, snapshotUrl ++ releaseFolder, sonatypeCredentials
    )
  }
  def publishSigned: Unit = {
    lib.publishSigned(
      sourceFiles, pom +: `package`, releaseUrl ++ releaseFolder, sonatypeCredentials
    )
  }
}
