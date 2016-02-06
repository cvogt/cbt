import cbt.paths._
import scala.collection.immutable.Seq

object Main{
  // micro framework  
  var successes = 0
  var failures = 0
  def assert(condition: Boolean, msg: String = null) = {
    scala.util.Try{
      Predef.assert(condition, msg)
    }.map{ _ =>
      print(".")
      successes += 1
    }.recover{
      case e: AssertionError =>
        println("FAILED")
        e.printStackTrace
        failures += 1
    }.get
  }

  def runCbt(path: String, args: Seq[String]) = {
    import java.io._
    val allArgs = ((cbtHome + "/cbt") +: args)
    val pb = new ProcessBuilder( allArgs :_* )
    pb.directory(new File(cbtHome + "/test/" + path))
    val p = pb.start    
    val berr = new BufferedReader(new InputStreamReader(p.getErrorStream));
    val bout = new BufferedReader(new InputStreamReader(p.getInputStream));
    p.waitFor
    import collection.JavaConversions._
    val err = Stream.continually(berr.readLine()).takeWhile(_ != null).mkString("\n")
    val out = Stream.continually(bout.readLine()).takeWhile(_ != null).mkString("\n")
    Result(out, err, p.exitValue == 0)
  }
  case class Result(out: String, err: String, exit0: Boolean)
  def assertSuccess(res: Result) = {
    assert(res.exit0,res.toString)
  }

  // tests
  def usage(path: String) = {
    val usageString = "Methods provided by CBT"
    val res = runCbt(path, Seq())
    assert(res.out == "", res.out)
    assert(res.err contains usageString, res.err)
  }
  def compile(path: String) = {
    val res = runCbt(path, Seq("compile"))
    assertSuccess(res)
    // assert(res.err == "", res.err) // FIXME: enable this
  }
  def main(args: Array[String]): Unit = {
    import cbt._

    println("Running tests ")
    
    usage("nothing")
    compile("nothing")

    {
      val logger = new Logger(Set[String]())
      val noContext = Context(cbtHome + "/test/" + "nothing",Seq(),logger)
      val b = new Build(noContext){
        override def dependencies = Seq(
          MavenDependency("net.incongru.watchservice","barbary-watchservice","1.0")(logger),
          MavenDependency("net.incongru.watchservice","barbary-watchservice","1.0")(logger)
        )
      }
      val cp = b.classpath
      assert(cp.strings.distinct == cp.strings, "duplicates in classpath: "+cp)
    }

    println(" DONE!")
    println(successes+" succeeded, "+ failures+" failed" )
  }
}
