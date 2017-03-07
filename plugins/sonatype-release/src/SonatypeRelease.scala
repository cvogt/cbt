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
trait SonatypeRelease extends Publish{
  protected def sonatypeLib = SonatypeLib(groupId)

  def publishSonatype = sonatypeLib.publishSigned( publishedArtifacts, releaseFolder )

  override def publish = super.publish ++ publishSonatype
}
