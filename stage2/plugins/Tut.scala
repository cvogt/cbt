package cbt
trait Tut extends BaseBuild {
  def tut = Tut.apply( lib, context.cbtLastModified, context.paths.mavenCache, scalaMajorVersion ).config(
    projectDirectory / "tut", target / "tut", classpath, scalacOptions
  )
}

import java.io.File
object Tut {
  case class apply(
    lib: Lib, cbtLastModified: Long, mavenCache: File, scalaMajorVersion: String
  )(
    implicit
    logger: Logger, transientCache: java.util.Map[AnyRef, AnyRef], classLoaderCache: ClassLoaderCache
  ) {
    case class config(
      sourceDirectory:    File,
      targetDirectory:    File,
      classpath:          ClassPath,
      scalacOptions:      Seq[String],
      fileExtensionRegex: String      = """.*\.(md|txt|htm|html)""",
      version:            String      = "0.4.8"
    ) {
      def apply =
        lib.redirectOutToErr {
          tut( version ).runMain(
            "tut.TutMain",
            Seq(
              sourceDirectory.string, targetDirectory.string, fileExtensionRegex, "-cp", classpath.string
            ) ++ scalacOptions
          )
        }
    }
    def tut( version: String ) = MavenResolver( cbtLastModified, mavenCache, bintray( "tpolecat" ), mavenCentral ).bindOne(
      MavenDependency( "org.tpolecat", "tut-core_" ~ scalaMajorVersion, version )
    )
  }
}
