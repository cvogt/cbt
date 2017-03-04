package cbt.sonatype

import java.util.Base64

import scala.xml.XML

/**
  * Interface for Sonatype staging plugin HTTP API.
  * All resources are described here:
  * https://oss.sonatype.org/nexus-staging-plugin/default/docs/index.html
  *
  * Publish proccess via HTTP API described here:
  * https://support.sonatype.com/hc/en-us/articles/213465868-Uploading-to-a-Staging-Repository-via-REST-API?page=1#comment_204178478
  */
final class SonatypeHttpApi(sonatypeURI: String, sonatypeCredentials: String, profileName: String)(log: String => Unit) {
  import HttpUtils._

  private val base64Credentials = new String(Base64.getEncoder.encode(sonatypeCredentials.getBytes))

  // https://oss.sonatype.org/nexus-staging-plugin/default/docs/path__staging_profiles.html
  def getStagingProfile: StagingProfile = {
    log(s"Retrieving info for profile: $profileName")
    try{
      val (_, response) = GET(
        uri = s"$sonatypeURI/staging/profiles",
        headers = Map("Authorization" -> s"Basic $base64Credentials")
      )

      val currentProfile = (XML.loadString(response) \\ "stagingProfile" find { profile =>
        (profile \ "name").headOption.exists(_.text == profileName)
      }).getOrElse(throw new Exception(s"Failed to get profile with name: $profileName"))

      StagingProfile(
        id = (currentProfile \ "id").head.text,
        name = (currentProfile \ "name").head.text,
        repositoryTargetId = (currentProfile \ "repositoryTargetId").head.text,
        resourceURI = (currentProfile \ "resourceURI").head.text
      )
    } catch {
      case e: Exception => throw new Exception(s"Failed to get info for profile: $profileName", e)
    }
  }

  // https://oss.sonatype.org/nexus-staging-plugin/default/docs/path__staging_profile_repositories_-profileIdKey-.html
  def getStagingRepos(profile: StagingProfile): Seq[StagingRepository] = {
    log(s"Retrieving staging repositories for profile: $profileName")
    val (_, response) = GET(
      uri = s"$sonatypeURI/staging/profile_repositories/${profile.id}",
      headers = Map(
        "Authorization" -> s"Basic $base64Credentials"
      )
    )

    (XML.loadString(response) \\ "stagingProfileRepository") map extractStagingRepository
  }

  // https://oss.sonatype.org/nexus-staging-plugin/default/docs/path__staging_repository_-repositoryIdKey-.html
  def getStagingRepoById(repoId: StagingRepositoryId): StagingRepository = {
    log(s"Retrieving staging repo with id: ${repoId.repositoryId}")
    val (_, response) = GET(
      uri = s"$sonatypeURI/staging/repository/${repoId.repositoryId}",
      headers = Map(
        "Authorization" -> s"Basic $base64Credentials"
      )
    )

    extractStagingRepository(XML.loadString(response))
  }

  // https://oss.sonatype.org/nexus-staging-plugin/default/docs/path__staging_profiles_-profileIdKey-_start.html
  def createStagingRepo(profile: StagingProfile): StagingRepositoryId = {
    log(s"Creating staging repositories for profile: $profileName")
    val (responseCode, response) = POST(
      uri = profile.resourceURI + "/start",
      body = createRequestBody("CBT staging repository").getBytes,
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

  // https://oss.sonatype.org/nexus-staging-plugin/default/docs/path__staging_profiles_-profileIdKey-_finish.html
  private def closeStagingRepo(profile: StagingProfile, repoId: StagingRepositoryId): Unit = {
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

  // https://oss.sonatype.org/nexus-staging-plugin/default/docs/path__staging_profiles_-profileIdKey-_promote.html
  // You can promote repository only when it is in "closed" state.
  private def promoteStagingRepo(profile: StagingProfile, repoId: StagingRepositoryId): Unit = {
    log(s"Promoting staging repo: ${repoId.repositoryId}")
    val responseCode = withRetry {
      // need to get fresh info about this repo
      val repoState = try getStagingRepoById(repoId) catch {
        case e: Exception =>
          throw new Exception(s"Repository with id ${repoId.repositoryId} not found. Maybe it was dropped already", e)
      }

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

  // https://oss.sonatype.org/nexus-staging-plugin/default/docs/path__staging_profiles_-profileIdKey-_drop.html
  // It's safe to drop repository in "released" state.
  private def dropStagingRepo(profile: StagingProfile, repoId: StagingRepositoryId): Unit = {
    log(s"Dropping staging repo: ${repoId.repositoryId}")
    val responseCode = withRetry {
      // need to get fresh info about this repo
      val repoState = try getStagingRepoById(repoId) catch {
        case e: Exception =>
          throw new Exception(s"Repository with id ${repoId.repositoryId} not found. Maybe it was dropped already", e)
      }

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

  private def promoteRequestBody(repoId: String, description: String, targetRepoId: String) =
    s"""
      <promoteRequest>
        <data>
          <stagedRepositoryId>$repoId</stagedRepositoryId>
          <description>$description</description>
          <targetRepositoryId>$targetRepoId</targetRepositoryId>
        </data>
      </promoteRequest>
    """


  private def createRequestBody(description: String) =
    s"""
      <promoteRequest>
        <data>
          <description>$description</description>
        </data>
      </promoteRequest>
    """

  private def extractStagingRepository(repo: xml.Node): StagingRepository =
    StagingRepository(
      (repo \ "profileId").head.text,
      (repo \ "profileName").head.text,
      (repo \ "repositoryId").head.text,
      RepositoryState.fromString((repo \ "type").head.text)
    )
}

