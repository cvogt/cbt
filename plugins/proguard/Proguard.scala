package cbt

trait ProGuard extends BaseBuild {
  def proguard: () => ClassPath
  def ProGuard(keep: (Seq[cbt.proguard.KeepOptionModifier], String)) = {
    cbt.ProGuard(context).proguard(
      outjars = Some( Seq(scalaTarget / "proguarded.jar") ),
      injars = Some( classpath.files ),
      libraryjars = Some( ClassPath( cbt.proguard.ProGuard.`rt.jar` ).files ),
      keep = Some( keep )
    )
  }
}

object ProGuard {
  def apply( implicit context: Context ) = {
    import context._
    val lib = new Lib(context.logger)
    import cbt.proguard.ProGuard._
    cbt.proguard.ProGuard(
      (args: Seq[String]) => MavenResolver(
        cbtLastModified, context.paths.mavenCache, mavenCentral
      )(
        context.logger, transientCache, context.classLoaderCache
      ).bindOne(
        MavenDependency(groupId, artifactId, version)
      ).runMain(cbt.proguard.ProGuard.mainClass, args).integer,
      ClassPath(_),
      context.logger.log("proguard",_)
    )
  }
}
