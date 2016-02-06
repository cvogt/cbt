package cbt
class AdminTasks(lib: Lib, args: Array[String]){
  def resolve = {
    ClassPath.flatten(
      args(0).split(",").toVector.map{
        d =>
          val v = d.split(":")
          new MavenDependency(v(0),v(1),v(2))(lib.logger).classpath
      }
    )
  }
}
