package macroparadise_example_build
import cbt._

trait SharedSettings extends NewStyleMacros{
  override def defaultScalaVersion = "2.12.2"
}

class Build(val context: Context) extends SharedSettings{
  override def dependencies =
    super.dependencies :+
    new MacroBuild( context.copy( workingDirectory = projectDirectory / "macros" ) )
}

class MacroBuild(val context: Context) extends SharedSettings{
  override def dependencies =
    super.dependencies :+
    Resolver( mavenCentral ).bindOne( ScalaDependency( "org.scalameta", "scalameta", "1.8.0" ) )
}

