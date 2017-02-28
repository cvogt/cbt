import cbt._
class Build(val context: Context) extends SbtLayoutMain {
  outer =>
  /* FIXME: calling `cbt rt` for `examples/scalatest-example` leads to
java.lang.Exception: This should never happend. Could not find (org.scala-lang,scala-reflect) in
(org.scala-lang,scala-library) -> BoundMavenDependency(1488121318000,cbt/cache/maven,MavenDependency(org.scala-lang,scala-library,2.11.8,Classifier(None)),Vector(https://repo1.maven.org/maven2))
  at cbt.Stage1Lib$$anonfun$actual$1.apply(Stage1Lib.scala:425)
  */
  override def test: Dependency = {
    new BasicBuild(context) with ScalaTest with SbtLayoutTest{
      override def dependencies = outer +: super.dependencies 
    }
  }
}
