import cbt._
import cbt.paths._
import scala.collection.immutable.Seq

// micro framework
object Main{
  def main(_args: Array[String]): Unit = {
    val args = new Stage1ArgsParser(_args.toVector)
    implicit val logger: Logger = new Logger(args.enabledLoggers)
    
    var successes = 0
    var failures = 0
    def assertException[T:scala.reflect.ClassTag](msg: String = "")(code: => Unit)(implicit logger: Logger) = {
      try{ 
        code
        assert(false, msg)
      }catch{ case _:AssertionError => }
    }
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

    def runCbt(path: String, _args: Seq[String])(implicit logger: Logger): Result = {
      import java.io._
      val allArgs: Seq[String] = ((cbtHome.string ++ "/cbt") +: "direct" +: (_args ++ args.propsRaw))
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
      assert(res.exit0, msg ++ res.toString)
    }

    // tests
    def usage(path: String)(implicit logger: Logger) = {
      val usageString = "Methods provided by CBT"
      val res = runCbt(path, Seq())
      logger.test(res.toString)
      val debugToken = "usage " ++ path ++ " "
      assertSuccess(res,debugToken)
      assert(res.out == "", debugToken ++ res.toString)
      assert(res.err contains usageString, debugToken ++ res.toString)
    }
    def compile(path: String)(implicit logger: Logger) = {
      val res = runCbt(path, Seq("compile"))
      val debugToken = "compile " ++ path ++ " "
      assertSuccess(res,debugToken)
      // assert(res.err == "", res.err) // FIXME: enable this
    }

    logger.test( "Running tests " ++ _args.toList.toString )

    usage("nothing")
    compile("nothing")
    usage("multi-build")
    compile("multi-build")
    usage("simple")
    compile("simple")
    
    {
      val noContext = Context(cbtHome ++ "/test/nothing", Seq(), logger, false, new ClassLoaderCache(logger))
      val b = new Build(noContext){
        override def dependencies = Seq(
          JavaDependency("net.incongru.watchservice","barbary-watchservice","1.0"),
          JavaDependency("net.incongru.watchservice","barbary-watchservice","1.0")
        )
      }
      val cp = b.classpath
      assert(cp.strings.distinct == cp.strings, "duplicates in classpath: " ++ cp.string)
    }

    // test that messed up artifacts crash with an assertion (which should tell the user what's up)
    assertException[AssertionError](){
      JavaDependency("com.jcraft", "jsch", " 0.1.53").classpath
    }
    assertException[AssertionError](){
      JavaDependency("com.jcraft", null, "0.1.53").classpath
    }
    assertException[AssertionError](){
      JavaDependency("com.jcraft", "", " 0.1.53").classpath
    }
    assertException[AssertionError](){
      JavaDependency("com.jcraft%", "jsch", " 0.1.53").classpath
    }
    assertException[AssertionError](){
      JavaDependency("", "jsch", " 0.1.53").classpath
    }
    

    System.err.println(" DONE!")
    System.err.println( successes.toString ++ " succeeded, "++ failures.toString ++ " failed" )
    if(failures > 0) System.exit(1) else System.exit(0)
  }
}
