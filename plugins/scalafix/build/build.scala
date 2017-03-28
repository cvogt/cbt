package scalafix_build

import cbt._

class Build(val context: Context) extends Plugin {
  override def dependencies = super.dependencies ++ Seq(
    //plugins.scalameta
  ) :+ Resolver( mavenCentral ).bindOne(
    ScalaDependency(
      "ch.epfl.scala", "scalafix-core", "0.3.2"
    )
  ).copy(
    // required until https://github.com/scalacenter/scalafix/issues/100 is fixed
    replace = _ => _.flatMap{
      case m@MavenDependency("org.scalameta", artifactId,_,_,_)
        if (artifactId startsWith "scalahost_")
        || (artifactId startsWith "contrib_")
        => Seq( m )
      case MavenDependency("org.scalameta", _,_,_,_) => Seq(
        MavenDependency(
          "org.scalameta", "scalahost_" ++ scalaVersion, "1.6.0"
        )
      )
      case other => Seq( other )
    }
  )
}
