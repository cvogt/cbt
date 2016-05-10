/*
package cbt
object Coursier{
  implicit class CoursierDependencyResolution(d: JavaDependency){
    import d._
    def resolveCoursier = {
      import coursier._
      val repositories = Seq(
        Cache.ivy2Local,
        MavenResolver("https://repo1.maven.org/maven2")
      )

      val start = Resolution(
        Set(
          JavaDependency(
            Module(groupId, artifactId), version
          )
        )
      )

      val fetch = Fetch.from(repositories, Cache.fetch())


      val resolution = start.process.run(fetch).run

      val errors: Seq[(JavaDependency, Seq[String])] = resolution.errors

      if(errors.nonEmpty) throw new Exception(errors.toString)

      import java.io.File
      import scalaz.\/
      import scalaz.concurrent.Task

      val localArtifacts: Seq[FileError \/ File] = Task.gatherUnordered(
        resolution.artifacts.map(Cache.file(_).run)
      ).run

      val files = localArtifacts.map(_.toEither match {
        case Left(error) => throw new Exception(error.toString)
        case Right(file) => file
      })

      resolution.dependencies.map( d => cbt.JavaDependency(d.module.organization,d.module.name, d.version)).to[collection.immutable.Seq]
    }
  }
}
*/