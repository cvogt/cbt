package cbt.sonatype

import java.io.File
import java.net.URL
import java.nio.file.Files._
import java.nio.file.Paths

import cbt.{ ExitCode, Lib, Logger }

import scala.util.{ Failure, Success, Try }

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

  /*
   * Signed publish steps:
   * • create new staging repo
   * • create artifacts and sign them
   * • publish jars to created repo
  */
  def sonatypePublishSigned(
                             sourceFiles: Seq[File],
                             artifacts: Seq[File],
                             sonatypeServiceURI: String,
                             sonatypeSnapshotsURI: String,
                             profileName: String,
                             groupId: String,
                             artifactId: String,
                             version: String,
                             isSnapshot: Boolean,
                             scalaMajorVersion: String
                                 )(lib:Lib): ExitCode = {
    if(sourceFiles.nonEmpty) {
      System.err.println(lib.blue("Staring publishing to Sonatype."))
      val sonatypeApi = new SonatypeHttpApi(sonatypeServiceURI, sonatypeCredentials, profileName)(log(lib.logger))

      val profile = Try(sonatypeApi.getStagingProfile) match {
        case Success(p) => p
        case Failure(e) => throw new Exception(s"Failed to get info for profile: $profileName", e)
      }

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
                     sonatypeURI: String,
                     profileName: String,
                     groupId: String,
                     artifactId: String,
                     version: String
                     )(lib: Lib): ExitCode = {
    val sonatypeApi = new SonatypeHttpApi(sonatypeURI, sonatypeCredentials, profileName)(log(lib.logger))

    val profile = Try(sonatypeApi.getStagingProfile) match {
      case Success(p) => p
      case Failure(e) => throw new Exception(s"Failed to get info for profile: $profileName", e)
    }

    val repos = sonatypeApi.getStagingRepos(profile).toList

    repos match {
      case Nil =>
        System.err.println(lib.red("No staging repositories found, you need to publish artifacts first."))
        ExitCode.Failure
      case repo :: Nil =>
        sonatypeApi.finishRelease(repo, profile)
        System.err.println(lib.green(s"Successfully released ${groupId}/${artifactId} v:${version}"))
        ExitCode.Success
      case _ =>
        val desc = (repos.zipWithIndex map { case (r, i) =>
          s"  [${i + 1}] ${r.repositoryId} in state: ${r.state}"
        }).mkString("\n", "\n", "\n")

        lazy val repoNumber =
          lib
            .consoleOrFail(lib.red("Provide name of one repository you want to release via comand line args, like `cbt sonatypeRelease myawesomeprofile-1004`"))
            .readLine(lib.blue(s"More than one staging repo detected. Select one by it's number:") + desc)
            .mkString

        Try(repos(repoNumber.toInt - 1)) match {
          case Success(repo) =>
            sonatypeApi.finishRelease(repo, profile)
            System.err.println(lib.green(s"Successfully released ${groupId}/${artifactId} v:${version}"))
            ExitCode.Success
          case Failure(_) =>
            System.err.println(lib.red("Wrong repository number, try again please."))
            ExitCode.Failure
        }
    }
  }

  private def log(logger: Logger): String => Unit = logger.log("Sonatype", _)

}
