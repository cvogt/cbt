package cbt_examples_build.cross_rewrite
import cbt._
import java.io.File
import scala.meta._
import scalafix.util._
import scalafix.util.TreePatch._
import scalafix.util.TokenPatch._

class Build(val context: Context) extends BaseBuild with Scalameta { defaultMainBuild =>
  override def defaultScalaVersion: String = "2.12.1"

  override def test: BasicBuild = {
    new BasicBuild(context) with ScalaTest with Scalameta {
      override def dependencies = defaultMainBuild +: super.dependencies
      override def defaultScalaVersion = defaultMainBuild.scalaVersion
      override def projectDirectory = {
        val d = defaultMainBuild.projectDirectory / "test"
        d.mkdirs
        d
      }
    }
  }

  import CrossRewrite._

  def cross = CrossRewrite.versions.flatMap{ case ( v, version_rewrites ) =>
    CrossRewrite.libs.map{
      case ( label, dep, lib_rewrites ) =>
        val d = defaultMainBuild.target / "rewrites" / label ++ "-" ++ v
        d.mkdirs
        new Build(context) with Scalafix with PackageJars{ patchedMainBuild =>
          override def groupId = "org.cvogt"
          override def artifactId = "cbt-examples-cross-rewrite-" + label
          override def version = "0.1"
          override def defaultScalaVersion = v
          override def dependencies =
            super.dependencies ++ Resolver(mavenCentral).bind(
              // hack because using ScalaDependency in the outer build binds it
              // to THAT builds initial scalaVersion, which we are overriding
              // here, but we are looping over libs outside of that, so
              // the override doesn't affect it
              // So we use MavenDependency instead and append the id here.
              dep.copy(artifactId = dep.artifactId + "_" + scalaMajorVersion)
            )
          override def projectDirectory = d
          override def scaladoc = None
          override def sources = CrossRewrite.patchesSources(
            defaultMainBuild.sources,
            projectDirectory / "src",
            defaultMainBuild.classpath,
            lib_rewrites ++ version_rewrites,
            lib
          )

          override def test = {
            new BasicBuild( context ) with ScalaTest { patchedTestBuild =>
              override def defaultScalaVersion = v
              override def projectDirectory = {
                val dt = defaultMainBuild.projectDirectory / "test"
                dt.mkdirs
                dt
              }
              override def dependencies = patchedMainBuild +: super.dependencies
              override def target = super.target / v ~ "-" ~ label

              override def sources = CrossRewrite.patchesSources(
                defaultMainBuild.test.sources,
                target / "src",
                defaultMainBuild.test.classpath,
                lib_rewrites ++ version_rewrites,
                lib
              )
            }
          }
        }
    }
  }
}

object CrossRewrite{
  def versions = Seq[(String, Seq[Patch])](
    "2.12.1" -> Seq(),
    "2.11.8" -> Seq(
      RemoveGlobalImport(
        importer"scala.concurrent.Future"
      ),
      AddGlobalImport(
        importer"scala.util.Try"
      )
    )
  )
  def libs = Seq[(String, MavenDependency, Seq[Patch])](
    (
      "scalaz",
      MavenDependency( "org.scalaz", "scalaz-core", "7.2.10" ),
      Seq(
        AddGlobalImport(importer"scalaz._"),
        Replace(Symbol("_root_.scala.package.Either."), q"\/"),
        Replace(Symbol("_root_.scala.util.Right."), q"\/-"),
        RemoveGlobalImport(importer"cats.implicits._")
      )
    ),
    (
      "cats",
      MavenDependency( "org.typelevel", "cats", "0.9.0" ),
      Seq(
        AddGlobalImport(importer"cats.implicits._")
      )
    )
  )

  def patchesSources(
    sources: Seq[File],
    destination: File,
    semanticDbClassPath: ClassPath,
    patches: Seq[Patch],
    lib: Lib
  ) = {
    val fromTo = lib.autoRelative( sources ).collect{
      case (location, relative) if location.isFile
      => location -> destination / relative
    }

    val to = fromTo.map(_._2)
    assert( ( to diff to.distinct ).isEmpty )

    Scalafix.apply(lib).config(
      semanticDbClassPath,
      files = fromTo,
      patches = patches,
      allowEmpty = true
    ).apply

    to
  }
}