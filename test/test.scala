import cbt._
import cbt.paths._
import scala.collection.immutable.Seq

// micro framework
object Main{
  def main(args: Array[String]): Unit = {
    val init = new Init(args)
    implicit val logger: Logger = init.logger
    
    var successes = 0
    var failures = 0
    def assert(condition: Boolean, msg: String = "")(implicit logger: Logger) = {
      scala.util.Try{
        Predef.assert(condition, "["++msg++"]")
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

    def runCbt(path: String, args: Seq[String])(implicit logger: Logger): Result = {
      import java.io._
      val allArgs: Seq[String] = ((cbtHome.string ++ "/cbt") +: "direct" +: (args ++ init.propsRaw))
      logger.test(allArgs.toString)
      val pb = new ProcessBuilder( allArgs :_* )
      pb.directory(cbtHome ++ ("/test/" ++ path))
      val p = pb.start
      val berr = new BufferedReader(new InputStreamReader(p.getErrorStream));
      val bout = new BufferedReader(new InputStreamReader(p.getInputStream));
      import collection.JavaConversions._
      val err = Stream.continually(berr.readLine()).takeWhile(_ != null).mkString("\n")
      val out = Stream.continually(bout.readLine()).takeWhile(_ != null).mkString("\n")
      p.waitFor
      Result(p.exitValue == 0, out, err)
    }
    case class Result(exit0: Boolean, out: String, err: String)
    def assertSuccess(res: Result, msg: => String)(implicit logger: Logger) = {
      assert(res.exit0, msg + res.toString)
    }

    // tests
    def usage(path: String)(implicit logger: Logger) = {
      val usageString = "Methods provided by CBT"
      val res = runCbt(path, Seq())
      logger.test(res.toString)
      val debugToken = "usage " + path +" "
      assertSuccess(res,debugToken)
      assert(res.out == "", debugToken+ res.toString)
      assert(res.err contains usageString, debugToken+res.toString)
    }
    def compile(path: String)(implicit logger: Logger) = {
      val res = runCbt(path, Seq("compile"))
      val debugToken = "compile " + path +" "
      assertSuccess(res,debugToken)
      // assert(res.err == "", res.err) // FIXME: enable this
    }

    logger.test( "Running tests " ++ args.toList.toString )

    usage("nothing")
    compile("nothing")
    usage("multi-build")
    compile("multi-build")
    usage("simple")
    compile("simple")
    
    {
      val noContext = Context(cbtHome ++ "/test/nothing", Seq(), logger)
      val b = new Build(noContext){
        override def dependencies = Seq(
          JavaDependency("net.incongru.watchservice","barbary-watchservice","1.0"),
          JavaDependency("net.incongru.watchservice","barbary-watchservice","1.0")
        )
      }
      val cp = b.classpath
      assert(cp.strings.distinct == cp.strings, "duplicates in classpath: " ++ cp.string)
    }

    System.err.println(" DONE!")
    System.err.println( successes.toString ++ " succeeded, "++ failures.toString ++ " failed" )
    if(failures > 0) System.exit(1) else System.exit(0)
  }
}
