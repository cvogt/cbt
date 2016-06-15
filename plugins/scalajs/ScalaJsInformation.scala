import cbt._

trait ScalaJsInformation extends BasicBuild { outer =>

  val sjsVersion = "0.6.8"
  final private val sjsMajorVersion: String = lib.libMajorVersion(sjsVersion)
  final protected val artifactIdSuffix = s"_sjs$sjsMajorVersion"

  final protected val scalaJsCompilerDep =
    Resolver( mavenCentral ).bindOne(
      // Has to be full Scala version because the compiler is incompatible between versions
      MavenDependency("org.scala-js", "scalajs-compiler_2.11.8", sjsVersion)
    )

  final protected val scalaJsLibDep =
    Resolver( mavenCentral ).bindOne(
      ScalaDependency("org.scala-js", "scalajs-library", sjsVersion)
    )

  final protected val scalaJsCliDep =
    Resolver( mavenCentral ).bindOne(
      ScalaDependency("org.scala-js", "scalajs-cli", sjsVersion)
    )
}

