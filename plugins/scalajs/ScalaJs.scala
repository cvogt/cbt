import java.io.File

import cbt._


trait ScalaJsSbtDependencyDsl extends SbtDependencyDsl { self: ScalaJsBuild =>

  //Note: We make same assumption about scala version.
  //In order to be able to choose different scala version, one has to use %.
  implicit class ScalaJsDependencyBuilder(groupId: String){
    def %%%(artifactId: String) = new DependencyBuilder2(
      groupId, artifactId + artifactIdSuffix, Some(scalaMajorVersion))
  }
}

trait ScalaJsBuild extends BaseBuild with ScalaJsSbtDependencyDsl with ScalaJsInformation { outer =>

  def sharedFolder = projectDirectory ++ "/shared"
  def jvmFolder = projectDirectory ++ "/jvm"
  def jsFolder = projectDirectory ++ "/js"

  private lazy val jvmBuild = new BasicBuild(outer.context){
    override def sources = Seq(sharedFolder ++ "/src/main/scala", jvmFolder ++ "/src/main/scala")
    override def target = jvmFolder ++ "/target"
    override def dependencies = outer.dependencies ++ jvmDependencies
  }
  private lazy val jsBuild = new BasicBuild(outer.context){
    override def sources = Seq(sharedFolder ++ "/src/main/scala", jsFolder ++ "/src/main/scala")
    override def target = jsFolder ++ "/target"
    override def dependencies = outer.dependencies :+ scalaJsLibDep
    override def scalacOptions = super.scalacOptions ++
      Seq(s"-Xplugin:${scalaJsCompilerDep.jar.getAbsolutePath}", "-Xplugin-require:scalajs")
  }

  def jvmDependencies = Seq.empty[Dependency]
  //TODO: implement
  def jsDependencies = Seq.empty[Dependency]
  def jvmCompile: Option[File] = jvmBuild.compile
  def jsCompile: Option[File] = jsBuild.compile
  override def compile = {
    jvmCompile
    jsCompile
  }

  trait JsOutputMode {
    def option: String
    def fileSuffix: String
  }
  case object FastOptJS extends JsOutputMode{
    override val option = "-f"
    override val fileSuffix = "fastopt"
  }
  case object FullOptJS extends JsOutputMode{
    override val option = "-u"
    override val fileSuffix = "fullopt"
  }

  private def output(mode: JsOutputMode) = s"${jsBuild.target.getAbsolutePath}/$projectName-${mode.fileSuffix}.js"

  //TODO: should process all options that Scalajsld recognizes?
  private def link(mode: JsOutputMode, outputPath: String) = {
    lib.runMain(
      "org.scalajs.cli.Scalajsld",
      Seq(
        mode.option,
        "-s",
        "--stdlib", s"${scalaJsLibDep.jar.getAbsolutePath}",
        "-o", outputPath,
        jsBuild.target.getAbsolutePath) ++
        jsBuild.dependencies.collect{case d: BoundMavenDependency => d.jar.getAbsolutePath},
      scalaJsCliDep.classLoader(jsBuild.context.classLoaderCache))
  }
  def fastOptJS = link(FastOptJS, fastOptOutput)
  def fullOptJS = link(FullOptJS, fullOptOutput)
  def fastOptOutput: String = output(FastOptJS)
  def fullOptOutput: String = output(FullOptJS)
}




