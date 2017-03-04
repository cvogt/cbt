package cbt.sonatype

import java.io.File
import java.net.URL
import java.nio.file.Files._
import java.nio.file.Paths

import cbt.{ ExitCode, Lib }

/**
  * Sonatype release process is:
  * • get your profile info to publish artifacts
  * • open staging repository to publish artifacts
  * • publish signed artifacts and signatures to staging repository
  * • close staging repository
  * • promote staging repository
  * • drop staging repository
  */

object SonatypeLib {

  val sonatypeServiceURI: String = "https://oss.sonatype.org/service/local"

  val sonatypeSnapshotsURI: String = "https://oss.sonatype.org/content/repositories/snapshots"

  /**
    * login:password for Sonatype access.
    * Order of credentials lookup:
    * • environment variables SONATYPE_USERNAME and SONATYPE_PASSWORD
    * • ~/.cbt/sonatype-credentials
    */
  def sonatypeCredentials: String = {
    def fromEnv = for {
      username <- Option(System.getenv("SONATYPE_USERNAME"))
      password <- Option(System.getenv("SONATYPE_PASSWORD"))
    } yield s"$username:$password"

    def fromFile = {
      for {
        home <- Option(System.getProperty("user.home"))
        credsPath = Paths.get(home, ".cbt", "sonatype-credentials")
      } yield new String(readAllBytes(credsPath)).trim
    }

    fromEnv
      .orElse(fromFile)
      .getOrElse(throw new Exception(
        "No Sonatype credentials found! You can provide them via SONATYPE_USERNAME, SONATYPE_PASSWORD env variables, " +
          "or in ~/.cbt/sonatype-credentials file as login:password"
      ))
  }
}

final class SonatypeLib(
  sonatypeServiceURI: String,
  sonatypeSnapshotsURI: String,
  sonatypeCredentials: String,
  profileName: String
)( lib: Lib, log: String => Unit ) {

  private val sonatypeApi = new SonatypeHttpApi(sonatypeServiceURI, sonatypeCredentials, profileName)(sonatypeLogger)

  /*
   * Signed publish steps:
   * • create new staging repo
   * • create artifacts and sign them
   * • publish jars to created repo
  */
  def sonatypePublishSigned(
    sourceFiles: Seq[File],
    artifacts: Seq[File],
    groupId: String,
    artifactId: String,
    version: String,
    isSnapshot: Boolean,
    scalaMajorVersion: String
  ): ExitCode = {
    if(sourceFiles.nonEmpty) {
      System.err.println(lib.blue("Staring publishing to Sonatype."))

      val profile = getStagingProfile()

      val deployURI = (if (isSnapshot) {
        sonatypeSnapshotsURI
      } else {
        val repoId = sonatypeApi.createStagingRepo(profile)
        s"${sonatypeServiceURI}/staging/deployByRepositoryId/${repoId.repositoryId}"
      }) + s"/${groupId.replace(".", "/")}/${artifactId}_${scalaMajorVersion}/${version}"

      lib.publishSigned(
        artifacts = artifacts,
        url = new URL(deployURI),
        credentials = Some(sonatypeCredentials)
      )
      System.err.println(lib.green("Successfully published artifacts to Sonatype."))
      ExitCode.Success
    } else {
      System.err.println(lib.red("Sources are empty, won't publish empty jar."))
      ExitCode.Failure
    }
  }

  /**
    * Release is:
    * • find staging repo related to current profile;
    * • close this staging repo;
    * • wait until this repo is released;
    * • drop this repo.
    */
  def sonatypeRelease(
    groupId: String,
    artifactId: String,
    version: String
  ): ExitCode = {
    val profile = getStagingProfile()

    sonatypeApi.getStagingRepos(profile).toList match {
      case Nil =>
        System.err.println(lib.red("No staging repositories found, you need to publish artifacts first."))
        ExitCode.Failure
      case repo :: Nil =>
        sonatypeApi.finishRelease(repo, profile)
        System.err.println(lib.green(s"Successfully released ${groupId}/${artifactId} v:${version}"))
        ExitCode.Success
      case repos =>
        val showRepo = { r: StagingRepository => s"${r.repositoryId} in state: ${r.state}" }
        val toRelease = lib.pickOne(lib.blue(s"More than one staging repo found. Select one of them:"), repos)(showRepo)

        toRelease map { repo =>
          sonatypeApi.finishRelease(repo, profile)
          System.err.println(lib.green(s"Successfully released ${groupId}/${artifactId} v:${version}"))
          ExitCode.Success
        } getOrElse {
          System.err.println(lib.red("Wrong repository number, try again please."))
          ExitCode.Failure
        }
    }
  }

  private def getStagingProfile() =
    try {
      sonatypeApi.getStagingProfile
    } catch {
      case e: Exception => throw new Exception(s"Failed to get info for profile: $profileName", e)
    }

  private def sonatypeLogger: String => Unit = lib.logger.log("Sonatype", _)

}
