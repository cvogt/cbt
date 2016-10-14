object Main{
  def main(args: Array[String]): Unit = {
    import BuildInfo._
    println("scalaVersion: "+scalaVersion)
    println("groupId: "+groupId)
    println("artifactId: "+artifactId)
    println("version: "+version)
  }
}