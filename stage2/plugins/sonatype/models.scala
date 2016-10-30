package cbt.sonatype

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
