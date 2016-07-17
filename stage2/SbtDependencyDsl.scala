package cbt
trait SbtDependencyDsl{ self: BaseBuild =>
  /** SBT-like dependency builder DSL for syntax compatibility */
  class DependencyBuilder2( groupId: String, artifactId: String, scalaVersion: Option[String] ){
    def %(version: String) = scalaVersion.map(
      v => ScalaDependency(groupId, artifactId, version, scalaVersion = v)
    ).getOrElse(
      MavenDependency(groupId, artifactId, version)
    )
  }
  implicit class DependencyBuilder(groupId: String){
    def %%(artifactId: String) = new DependencyBuilder2( groupId, artifactId, Some(scalaMajorVersion) )
    def  %(artifactId: String) = new DependencyBuilder2( groupId, artifactId, None )
  }
}