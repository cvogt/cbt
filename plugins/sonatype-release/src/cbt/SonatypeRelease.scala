package cbt

import cbt.sonatype.SonatypeLib

/**
  * Sonatype release plugin.
  * It provides ability to release your artifacts to Sonatype OSSRH
  * and publish to Central repository (aka Maven Central).
  *
  * Release proccess is executed in two steps:
  * • `sonatypePublishSigned`
  *     - creates staging repository to publish artifacts;
  *     - publishes signed artifacts(jars) to staging repository.
  * • `sonatypeRelease`
  *     - closes staging repository;
  *     - promotes staging repository to Central repository;
  *     - drops staging repository after release.
  */
trait SonatypeRelease extends Publish {

  def profileName: String = groupId

  def sonatypeServiceURI: String = SonatypeLib.sonatypeServiceURI

  def sonatypeSnapshotsURI: String = SonatypeLib.sonatypeSnapshotsURI

  def sonatypeCredentials: String = SonatypeLib.sonatypeCredentials

  def sonatypePublishSigned: ExitCode =
    SonatypeLib.sonatypePublishSigned(
      sourceFiles,
      `package` :+ pom,
      sonatypeServiceURI,
      sonatypeSnapshotsURI,
      profileName,
      groupId,
      artifactId,
      version,
      isSnapshot,
      scalaMajorVersion
    )(lib)

  def sonatypePublishSignedSnapshot: ExitCode = {
    copy(context.copy(version = Some(version + "-SNAPSHOT"))).sonatypePublishSigned
  }

  def sonatypeRelease: ExitCode =
    SonatypeLib.sonatypeRelease(
      sonatypeServiceURI,
      profileName,
      groupId,
      artifactId,
      version
    )(lib)

  override def copy(context: Context) = super.copy(context).asInstanceOf[SonatypeRelease]
}
