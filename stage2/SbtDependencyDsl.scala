package cbt

trait SbtDependencyDsl{ self: BasicBuild =>
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
  implicit class DependencyBuilder3(d: MavenDependency){
    def  %(classifier: String): MavenDependency = d//.copy(classifier = Classifier(Some(classifier)))
  }

  /*
  /** SBT-like dependency builder DSL for syntax compatibility */
  implicit class DependencyBuilder0(repository: Maven){
    def %(groupId: String) = new DependencyBuilder1b(repository, groupId)
  }
  implicit class DependencyBuilder1a(groupId: String){
    def %%(artifactId: String) = new DependencyBuilder2( Maven.central, groupId, artifactId, Some(scalaMajorVersion) )
    def  %(artifactId: String) = new DependencyBuilder2( Maven.central, groupId, artifactId, None )
  }
  class DependencyBuilder1b(repository: Maven, groupId: String){
    def %%(artifactId: String) = new DependencyBuilder2( repository, groupId, artifactId, Some(scalaMajorVersion) )
    def  %(artifactId: String) = new DependencyBuilder2( repository, groupId, artifactId, None )
  }
  class DependencyBuilder2( repository: Maven, groupId: String, artifactId: String, scalaMajorVersion: Option[String] ){
    def %(version: String) = scalaMajorVersion.map(
      v => repository(groupId, artifactId, version, scalaMajorVersion = v)
    ).getOrElse(
      repository.java(groupId, artifactId, version)
    )
  }
  implicit class DependencyBuilder3(d: MavenDependency){
    def  %(classifier: String) = d.copy(classifier = Classifier(Some(classifier)))
  }
  */
}