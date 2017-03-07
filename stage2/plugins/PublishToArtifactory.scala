package cbt
import java.net._
import java.io._
trait PublishToArtifactory extends PublishLocal{
  def Artifactory = cbt.Artifactory( lib, publishedArtifacts, releaseFolder )
}
case class Artifactory( lib: Lib, publishedArtifacts: Seq[File], releaseFolder: String ){
  case class withURL( url: URL, credentials: Option[String] = None ){
    def publishUnsigned = lib.publishUnsigned( publishedArtifacts, url ++ releaseFolder, credentials )
    def publishSigned   = lib.publishSigned( publishedArtifacts, url ++ releaseFolder, credentials )
  }
}
