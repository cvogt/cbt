package cbt

trait KindProjectorPlugin extends BaseBuild {
  def kindProjectorVersion = "0.9.4"

  override def scalacOptions = super.scalacOptions ++
    KindProjector.scalacOptions(
      KindProjector.dependencies( scalaMajorVersion, kindProjectorVersion, context.cbtLastModified, context.paths.mavenCache ).jar
    )
}

object KindProjector {
  def dependencies(
                    scalaMajorVersion: String, kindProjectorVersion: String, cbtLastModified: Long, mavenCache: java.io.File
                  )(
                    implicit logger: Logger, transientCache: java.util.Map[AnyRef,AnyRef], classLoaderCache: ClassLoaderCache
                  ) =
    MavenResolver( cbtLastModified, mavenCache, mavenCentral ).bindOne(
      MavenDependency(
        "org.spire-math", "kind-projector_"+scalaMajorVersion, kindProjectorVersion
      )
    )

  def scalacOptions( jarPath: File ) =
    Seq(
      "-Xplugin:" ++ jarPath.string
    )
}
