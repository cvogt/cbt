package cbt

import java.net.URL
import java.nio.file.Files._
import java.nio.file.Paths
import java.util.Base64

import scala.annotation.tailrec
import scala.language.postfixOps
import scala.util.{ Failure, Success, Try }
import scala.xml.XML

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

  // sonatype service uri
  def sonatypeRepoUri: String = "https://oss.sonatype.org/service/local"

  /**
    * login:password pair for Sonatype access.
    * Order of credentials lookup:
    * • environment variables SONATYPE_USERNAME and SONATYPE_PASSWORD
    * • ~/.sonatype/.credentials file
    */
  def sonatypeCredentials: String = {
    def fromEnv = for {
      username <- Option(System.getenv("SONATYPE_USERNAME"))
      password <- Option(System.getenv("SONATYPE_PASSWORD"))
    } yield s"$username:$password"

    def fromFile = {
      for {
        home <- Option(System.getProperty("user.home"))
        credsPath = Paths.get(home, ".sonatype/.credentials")
      } yield new String(readAllBytes(credsPath)).trim
    }

    fromEnv
      .orElse(fromFile)
      .getOrElse(throw new Exception(
        "No Sonatype credentials found! You can provide them via SONATYPE_USERNAME, SONATYPE_PASSWORD env variables, " +
          "or in ~/.sonatype/.credentials file as login:password"
      ))
  }

  /*
    * Signed publish steps:
    * • create new staging repo
    * • create artifacts and sign them
    * • publish jars to created repo
  */
  final def sonatypePublishSigned: ExitCode = {
    if(sourceFiles.nonEmpty) {
      System.err.println(lib.blue("Staring publishing to Sonatype."))
      val repoUri = sonatypeRepoUri
      val sonatype = new SonatypeLib(repoUri, sonatypeCredentials, profileName)(sonatypeLogger)

      val profile = sonatype.getStagingProfile
        .getOrElse(throw new Exception(s"Failed to get info for profile: $profileName"))
      val repoId = sonatype.createStagingRepo(profile)
      val uriPath = s"/${groupId.replace(".","/")}/${artifactId}_$scalaMajorVersion/$version"

      lib.publishSigned(
        artifacts = `package` :+ pom,
        url = new URL(
          s"${repoUri}/staging/deployByRepositoryId/${repoId.repositoryId}${uriPath}"
        ),
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
    * • find staging repo related to current profile.
    * • close this staging repo
    * • wait until this repo is released
    * • drop this repo
    */
  def sonatypeRelease: ExitCode = {
    val sonatype = new SonatypeLib(sonatypeRepoUri, sonatypeCredentials, profileName)(sonatypeLogger)
    val profile = sonatype.getStagingProfile
      .getOrElse(throw new Exception(s"Failed to get info for profile: $profileName"))

    val repos = sonatype.getStagingRepos(profile).toList

    repos match {
      case Nil =>
        System.err.println(lib.red("No staging repositories found, you need to publish artifacts first."))
        ExitCode.Failure
      case repo :: Nil =>
        sonatype.finishRelease(repo, profile)
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
            sonatype.finishRelease(repo, profile)
            System.err.println(lib.green(s"Successfully released ${groupId}/${artifactId} v:${version}"))
            ExitCode.Success
          case Failure(_) =>
            System.err.println(lib.red("Wrong repository number, try again please."))
            ExitCode.Failure
        }
    }
  }

  private def sonatypeLogger: String => Unit = lib.logger.log("Sonatype", _)

}

case class StagingProfile(
                           id: String,
                           name: String,
                           repositoryTargetId: String,
                           resourceURI: String
                         )

case class StagingRepositoryId(repositoryId: String)

object RepositoryState {
  val fromString: String => RepositoryState  = {
    case "open" => Open
    case "closed" => Closed
    case "released" => Released
    case other => Unknown(other)
  }
}
sealed trait RepositoryState
case object Open extends RepositoryState
case object Closed extends RepositoryState
case object Released extends RepositoryState
case class Unknown(state: String) extends RepositoryState

case class StagingRepository(
                              profileId: String,
                              profileName: String,
                              repositoryId: String,
                              state: RepositoryState // stands as `type` in XML response
                            )

/**
  * Sonatype release is:
  * • get your profile info to publish artifacts
  * • open staging repository to publish artifacts
  * • publish signed artifacts and signatures
  * • close staging repository
  * • promote staging repository
  * • drop staging repository
  */
private final class SonatypeLib(sonatypeUri: String, sonatypeCredentials: String, profileName: String)(log: String => Unit) {

  private val base64Credentials = new String(Base64.getEncoder.encode(sonatypeCredentials.getBytes))

  // TODO: is it possible to have multiple profiles with same name?
  def getStagingProfile: Option[StagingProfile] = {
    log(s"Retrieving profile info for profile: $profileName")
    val (_, response) = GET(
      uri = s"$sonatypeUri/staging/profiles",
      headers = Map("Authorization" -> s"Basic $base64Credentials")
    )

    val currentProfile = (XML.loadString(response) \\ "stagingProfile" find { profile =>
      (profile \ "name").headOption.exists(_.text == profileName)
    }).getOrElse(throw new Exception(s"Failed to get profile with name: $profileName"))

    for {
      id <- (currentProfile \ "id").headOption
      name <- (currentProfile \ "name").headOption
      repositoryTargetId <- (currentProfile \ "repositoryTargetId").headOption
      resourceURI <- (currentProfile \ "resourceURI").headOption
    } yield
      StagingProfile(
        id = id.text,
        name = name.text,
        repositoryTargetId = repositoryTargetId.text,
        resourceURI = resourceURI.text
      )
  }

  def getStagingRepos(profile: StagingProfile): Seq[StagingRepository] = {
    log(s"Retrieving staging repositories for profile: $profileName")
    val (_, response) = GET(
      uri = s"$sonatypeUri/staging/profile_repositories/${profile.id}",
      headers = Map(
        "Authorization" -> s"Basic $base64Credentials"
      )
    )

    (XML.loadString(response) \\ "stagingProfileRepository") flatMap extractStagingRepository
  }

  def getStagingRepoById(repoId: StagingRepositoryId): Option[StagingRepository] = {
    log(s"Retrieving staging repo with id: ${repoId.repositoryId}")
    val (_, response) = GET(
      uri = s"$sonatypeUri/staging/repository/${repoId.repositoryId}",
      headers = Map(
        "Authorization" -> s"Basic $base64Credentials"
      )
    )

    extractStagingRepository(XML.loadString(response))
  }

  def createStagingRepo(profile: StagingProfile): StagingRepositoryId = {
    log(s"Creating staging repositories for profile: $profileName")
    val (responseCode, response) = POST(
      uri = profile.resourceURI + "/start",
      body = createRequestBody("Create staging repository [CBT]").getBytes,
      headers = Map(
        "Authorization" -> s"Basic $base64Credentials",
        "Content-Type" -> "application/xml"
      )
    )

    require(responseCode == 201, s"Create staging repo response code. Expected: 201, got: $responseCode")

    val optRepositoryId = (XML.loadString(response) \ "data" \ "stagedRepositoryId").headOption.map(e => StagingRepositoryId(e.text))

    optRepositoryId.getOrElse(throw new Exception(s"Malformed response. Failed to get id of created staging repo"))
  }

  def finishRelease(repo: StagingRepository, profile: StagingProfile): Unit = {
    val repoId = StagingRepositoryId(repo.repositoryId)
    repo.state match {
      case Open =>
        closeStagingRepo(profile, repoId)
        promoteStagingRepo(profile, repoId)
        dropStagingRepo(profile, repoId)
      case Closed =>
        promoteStagingRepo(profile, repoId)
        dropStagingRepo(profile, repoId)
      case Released =>
        dropStagingRepo(profile, repoId)
      case Unknown(status) =>
        throw new Exception(s"Got repo in status: ${status}, can't finish release.")
    }
  }

  def closeStagingRepo(profile: StagingProfile, repoId: StagingRepositoryId): Unit = {
    log(s"Closing staging repo: ${repoId.repositoryId}")
    val (responseCode, _) = POST(
      uri = profile.resourceURI + "/finish",
      body = promoteRequestBody(
        repoId.repositoryId,
        "Close staging repository [CBT]",
        profile.repositoryTargetId
      ).getBytes,
      headers = Map(
        "Authorization" -> s"Basic $base64Credentials",
        "Content-Type" -> "application/xml"
      )
    )

    require(responseCode == 201, s"Close staging repo response code. Expected: 201, got: $responseCode")
  }

  def promoteStagingRepo(profile: StagingProfile, repoId: StagingRepositoryId): Unit = {
    log(s"Promoting staging repo: ${repoId.repositoryId}")
    val responseCode = withRetry {
      // need to get fresh info about this repo
      val repoState = getStagingRepoById(repoId)
        .getOrElse(throw new Exception(s"Repository with id ${repoId.repositoryId} not found. Maybe it was dropped already"))
      if(repoState.state == Closed) {
        val (code, _) = POST(
          uri = profile.resourceURI + "/promote",
          body = promoteRequestBody(
            repoId.repositoryId,
            "Promote staging repository [CBT]",
            profile.repositoryTargetId
          ).getBytes,
          headers = Map(
            "Authorization" -> s"Basic $base64Credentials",
            "Content-Type" -> "application/xml"
          )
        )
        code
      } else {
        throw new Exception(s"Can't promote, repository ${repoId.repositoryId} is not in closed state yet!")
      }
    }

    require(responseCode == 201, s"Promote staging repo response code. Expected: 201, got: $responseCode")
  }

  // check that repo is already released. It's safe to drop when repo is released.
  def dropStagingRepo(profile: StagingProfile, repoId: StagingRepositoryId): Unit = {
    log(s"Dropping staging repo: ${repoId.repositoryId}")
    val responseCode = withRetry {
      // need to get fresh info about this repo
      val repoState = getStagingRepoById(repoId)
        .getOrElse(throw new Exception(s"Repository with id ${repoId.repositoryId} not found. Maybe it was dropped already"))
      if (repoState.state == Released) {
        val (code, _) = POST(
          uri = profile.resourceURI + "/drop",
          body = promoteRequestBody(
            repoId.repositoryId,
            "Drop staging repository [CBT]",
            profile.repositoryTargetId
          ).getBytes,
          headers = Map(
            "Authorization" -> s"Basic $base64Credentials",
            "Content-Type" -> "application/xml"
          )
        )
        code
      } else {
        throw new Exception(s"Can't drop, repository ${repoId.repositoryId} is not in released state yet!")
      }
    }
    require(responseCode == 201, s"Drop staging repo response code. Expected: 201, got: $responseCode")
  }

  // ============== XML request bodies

  private def promoteRequestBody(repoId: String, description: String, targetRepoId: String) =
    s"""
      |<promoteRequest>
      |  <data>
      |    <stagedRepositoryId>$repoId</stagedRepositoryId>
      |    <description>$description</description>
      |    <targetRepositoryId>$targetRepoId</targetRepositoryId>
      |  </data>
      |</promoteRequest>
    """.stripMargin


  private def createRequestBody(description: String) =
    s"""
      |<promoteRequest>
      |  <data>
      |    <description>$description</description>
      |  </data>
      |</promoteRequest>
    """.stripMargin

  private def extractStagingRepository(repo: xml.Node): Option[StagingRepository] =
    for {
      profileId <- (repo \ "profileId").headOption
      profileName <- (repo \ "profileName").headOption
      repositoryId <- (repo \ "repositoryId").headOption
      repoType <- (repo \ "type").headOption
    } yield StagingRepository(
      profileId.text,
      profileName.text,
      repositoryId.text,
      RepositoryState.fromString(repoType.text)
    )


  // ============== HTTP request methods

  // Make http GET. On failure request will be retried with exponential backoff.
  private def GET(uri: String, headers: Map[String, String]): (Int, String) =
    withRetry(httpRequest("GET", uri, headers))

  // Make http POST. On failure request will be retried with exponential backoff.
  private def POST(uri: String, body: Array[Byte], headers: Map[String, String]): (Int, String) =
    withRetry(httpRequest("POST", uri, headers, body))

  private def httpRequest(method: String, uri: String, headers: Map[String, String], body: Array[Byte] = Array.emptyByteArray): (Int, String) = {
    val conn = Stage0Lib.openConnectionConsideringProxy(new URL(uri))
    conn.setReadTimeout(60000) // 1 minute
    conn.setConnectTimeout(30000) // 30 seconds

    headers foreach { case (k,v) =>
      conn.setRequestProperty(k, v)
    }
    conn.setRequestMethod(method)
    if(method == "POST" || method == "PUT") { // PATCH too?
      conn.setDoOutput(true)
      conn.getOutputStream.write(body)
    }

    val arr = new Array[Byte](conn.getContentLength)
    conn.getInputStream.read(arr)

    conn.getResponseCode -> new String(arr)
  }

  // ============== General utilities

  private def withRetry[T](f: => T): T = withRetry(4000, 5)(f)

  /**
    * Retry execution of `f` `retriesLeft` times
    * with `delay` doubled between attempts.
    */
  @tailrec
  private def withRetry[T](delay: Int, retriesLeft: Int)(f: ⇒ T): T = {
    Try(f) match {
      case Success(result) ⇒
        result
      case Failure(e) ⇒
        if (retriesLeft == 0) {
          throw new Exception(e)
        } else {
          val newDelay = delay * 2
          val newRetries = retriesLeft - 1
          log(s"Failed with exception: $e, will retry $newRetries times; waiting: $delay")
          Thread.sleep(delay)

          withRetry(newDelay, newRetries)(f)
        }
    }
  }

}
