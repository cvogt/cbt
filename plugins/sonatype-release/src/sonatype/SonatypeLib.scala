package cbt
package sonatype

import java.io.File
import java.net.URL
import java.nio.file.Files._
import java.nio.file.Paths

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

  val serviceURI: String = "https://oss.sonatype.org/service/local"

  val snapshotsURI: String = "https://oss.sonatype.org/content/repositories/snapshots"

  /**
    * login:password for Sonatype access.
    * Order of credentials lookup:
    * • environment variables SONATYPE_USERNAME and SONATYPE_PASSWORD
    * • ~/.cbt/sonatype-credentials
    */
  def credentials: String = {
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

final case class SonatypeLib(
  profileName: String,
  serviceURI: String = SonatypeLib.serviceURI,
  snapshotsURI: String = SonatypeLib.snapshotsURI,
  credentials: String = SonatypeLib.credentials // FIXME: maybe hide this from cli ?
)( implicit logger: Logger ){
  private val lib: Lib = new Lib(logger)
  private def log: String => Unit = logger.log("sonatype-release",_)

  val api = new SonatypeHttpApi(serviceURI, credentials, profileName)(log)
  /*
   * Signed publish steps:
   * • create new staging repo
   * • create artifacts and sign them
   * • publish jars to created repo
  */
  def publishSigned( artifacts: Seq[File], releaseFolder: String ) = {
    import api._
    System.err.println(lib.blue("Publishing to Sonatype"))

    def publish(deployURI: String) = lib.publishSigned(
      artifacts, new URL(deployURI ++ releaseFolder), Some(credentials)
    )

    if (releaseFolder.endsWith("-SNAPSHOT")){
      val urls = publish(snapshotsURI)
      System.err.println(lib.green("Successfully published snapshot on Sonatype!"))
      urls
    } else {
      val profile = getStagingProfile
      val repoId = createStagingRepo(profile)
      val urls = publish(
        serviceURI ++ "/staging/deployByRepositoryId/" ++ repoId.string
      )
      System.err.println(lib.green("Successfully uploaded jars to Sonatype!"))
      finishRelease( getStagingRepoById(repoId), profile )
      System.err.println(lib.green("Successfully released uploaded jars to Maven Central!"))
      urls
    }
  }

  /*
  /**
    * Release is:
    * • find staging repo related to current profile;
    * • close this staging repo;
    * • wait until this repo is released;
    * • drop this repo.
    */
  private def release: ExitCode = {
    val profile = getStagingProfile()

    System.err.println("Releasing jars to Maven Central!")

    getStagingRepos(profile).toList match {
      case Nil =>
        System.err.println(lib.red("No staging repositories found, you need to publish artifacts first."))
        ExitCode.Failure
      case repo :: Nil =>
        finishRelease(repo, profile)
        log(lib.green(s"Successfully released artifact"))
        ExitCode.Success
      case repos =>
        lib.pickOne(
          lib.blue(s"More than one staging repo found. Select one of them:"),
          repos
        ){ repo => s"${repo.repositoryId} in state: ${repo.state}" }.map{ repo =>
          finishRelease(repo, profile)
          log(lib.green(s"Successfully released artifact"))
          ExitCode.Success
        } getOrElse {
          System.err.println(lib.red("Wrong repository number, try again please."))
          ExitCode.Failure
        }
    }
  }
  */
}
