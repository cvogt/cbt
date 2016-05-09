package cbt

import scala.collection.immutable.Seq

trait SJSSbtDependencyDsl extends SbtDependencyDsl { self: SjsBuild =>
  implicit class SJSDependencyBuilder(groupId: String){
    def %%%(artifactId: String) = new DependencyBuilder2(groupId, artifactId + s"_sjs$sjsMajorVersion", Some(scalaMajorVersion))
  }
}

class SjsBuild(context: Context) extends BuildBuild(context) with SJSSbtDependencyDsl{

  val sjsVersion = "0.6.8"
  final val sjsMajorVersion: String = lib.libMajorVersion(sjsVersion)

  private val scalajsCompilerDep =
    MavenResolver(context.cbtHasChanged, context.paths.mavenCache, MavenResolver.central).resolveOne(
      //TODO: currrently maven version for compiler contains full scala version. Asked @sjrd why
      //Also this dependency is downloaded only if when sjsbuild is being actually used. not good!
      "org.scala-js" % "scalajs-compiler_2.11.8" % sjsVersion
    )
  private val scalajsLibDep =
    MavenResolver(context.cbtHasChanged, context.paths.mavenCache, MavenResolver.central).resolveOne(
      "org.scala-js" %% "scalajs-library" % sjsVersion
    )
  private val scalajsCliDep =
    MavenResolver(context.cbtHasChanged, context.paths.mavenCache, MavenResolver.central).resolveOne(
    "org.scala-js" %% "scalajs-cli"  % sjsVersion
  )
  private val thisDependencies = Seq(
    scalajsCompilerDep,
    scalajsLibDep,
    scalajsCliDep
  )
  override def dependencies: Seq[Dependency] =
    thisDependencies ++ super.dependencies
  private val sjsLinkCP =
    finalBuild.dependencies.diff(super.dependencies ++ thisDependencies).flatMap(_.dependencies).map{
      //TODO: should Dependency have file path so we don't have to cast to BoundMavenDependency ?
      case dep: BoundMavenDependency => dep.jar.getAbsolutePath
      case dep => throw new Error(s"Dependency unknown to SJS build: $dep")
    }
  override def scalacOptions = super.scalacOptions ++
      Seq(s"-Xplugin:${scalajsCompilerDep.jar.getAbsolutePath}", "-Xplugin-require:scalajs")
  //TODO: should process all options that Scalajsld recognizes
  def sjsLink = lib.runMain(
      "org.scalajs.cli.Scalajsld",
      Seq(
        "--stdlib", s"${scalajsLibDep.jar.getAbsolutePath}",
        "-o", s"${target().getAbsolutePath}/$projectName-fastopt.js",
        target().getAbsolutePath) ++ sjsLinkCP,
      scalajsCliDep.classLoader(context.classLoaderCache))
}