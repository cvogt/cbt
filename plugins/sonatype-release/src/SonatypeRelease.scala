package cbt
// TODO: maybe move this into stage2 to avoid having to call zinc separately for this as a plugin

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

  def sonatypePublishSigned: ExitCode = {
    sonatypeLib.sonatypePublishSigned(
      sourceFiles,
      `package` :+ pom,
      groupId,
      artifactId,
      version,
      isSnapshot,
      scalaMajorVersion
    )
  }

  def sonatypePublishSignedSnapshot: ExitCode = {
    copy(context.copy(version = Some(version + "-SNAPSHOT"))).sonatypePublishSigned
  }

  def sonatypeRelease: ExitCode =
    sonatypeLib.sonatypeRelease(groupId, artifactId, version)

  private def sonatypeLib =
    new SonatypeLib(sonatypeServiceURI, sonatypeSnapshotsURI, sonatypeCredentials, profileName)(lib, logger.log("sonatype-release",_))
}
