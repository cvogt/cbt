/*
// temporary debugging tool
package cbt
import java.util.concurrent.ConcurrentHashMap
import collection.JavaConversions._
object PoorMansProfiler{
  val entries = new ConcurrentHashMap[String, Long]
  def profile[T](name: String)(code: => T): T = {
    val before = System.currentTimeMillis
    if(!(entries containsKey name)){
      entries.put( name, 0 )
    }
    val res = code
    entries.put( name, (entries get name) + (System.currentTimeMillis - before) )
    res
  }
  def summary: String = {
    "Profiling Summary:\n" + entries.toSeq.sortBy(_._2).map{
      case (name, value) => name + ": " + (value / 1000.0)
    }.mkString("\n")
  }
}
*/