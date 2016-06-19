import cbt._
import java.util.concurrent.ConcurrentHashMap
import java.io.File
import java.net.URL

// micro framework
object Main{
  def main(_args: Array[String]): Unit = {
    val start = System.currentTimeMillis
    val args = new Stage1ArgsParser(_args.toVector)
    implicit val logger: Logger = new Logger(args.enabledLoggers, System.currentTimeMillis)
    val lib = new Lib(logger)
    val cbtHome = new File(System.getenv("CBT_HOME"))
    
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
    def compile(path: String)(implicit logger: Logger) = task("compile", path)
    def task(name: String, path: String)(implicit logger: Logger) = {
      val res = runCbt(path, Seq(name))
      val debugToken = name ++ " " ++ path ++ " "
      assertSuccess(res,debugToken)
      // assert(res.err == "", res.err) // FIXME: enable this
    }

    logger.test( "Running tests " ++ _args.toList.toString )

    val cache = cbtHome ++ "/cache"
    val mavenCache = cache ++ "/maven"
    val cbtHasChanged = true
    def Resolver(urls: URL*) = MavenResolver(cbtHasChanged, mavenCache, urls: _*)

    {
      val noContext = ContextImplementation(
        cbtHome ++ "/test/nothing",
        cbtHome,
        Array(),
        Array(),
        start,
        cbtHasChanged,
        null,
        null,
        new ConcurrentHashMap[String,AnyRef],
        new ConcurrentHashMap[AnyRef,ClassLoader],
        cache,
        cbtHome,
        cbtHome,
        cbtHome ++ "/compatibilityTarget",
        null
      )

      val b = new BasicBuild(noContext){
        override def dependencies =
          Resolver(mavenCentral).bind(
            MavenDependency("net.incongru.watchservice","barbary-watchservice","1.0"),
            MavenDependency("net.incongru.watchservice","barbary-watchservice","1.0")
          )
      }
      val cp = b.classpath
      assert(cp.strings.distinct == cp.strings, "duplicates in classpath: " ++ cp.string)
    }

    // test that messed up artifacts crash with an assertion (which should tell the user what's up)
    assertException[AssertionError](){
      Resolver(mavenCentral).bindOne( MavenDependency("com.jcraft", "jsch", " 0.1.53") ).classpath
    }
    assertException[AssertionError](){
      Resolver(mavenCentral).bindOne( MavenDependency("com.jcraft", null, "0.1.53") ).classpath
    }
    assertException[AssertionError](){
      Resolver(mavenCentral).bindOne( MavenDependency("com.jcraft", "", " 0.1.53") ).classpath
    }
    assertException[AssertionError](){
      Resolver(mavenCentral).bindOne( MavenDependency("com.jcraft%", "jsch", " 0.1.53") ).classpath
    }
    assertException[AssertionError](){
      Resolver(mavenCentral).bindOne( MavenDependency("", "jsch", " 0.1.53") ).classpath
    }

    (
      Dependencies(
        Resolver( mavenCentral, bintray("tpolecat") ).bind(
          lib.ScalaDependency("org.tpolecat","tut-core","0.4.2", scalaMajorVersion="2.11")
        )
      ).classpath.strings
      ++
     Dependencies(
      Resolver(sonatypeReleases).bind(
        MavenDependency("org.cvogt","play-json-extensions_2.11","0.8.0")
      )
    ).classpath.strings
      ++
      Dependencies(
        Resolver( mavenCentral, sonatypeSnapshots ).bind(
          MavenDependency("ai.x","lens_2.11","1.0.0")
        )
      ).classpath.strings
    ).foreach{
      path => assert(new File(path).exists, path)
    }

    usage("nothing")
    compile("nothing")
    usage("multi-build")
    compile("multi-build")
    usage("simple")
    compile("simple")
    usage("simple-fixed")
    compile("simple-fixed")
    
    compile("../plugins/sbt_layout")
    compile("../plugins/scalajs")
    compile("../plugins/scalatest")
    compile("../examples/scalatest-example")
    compile("../examples/scalajs-react-example/js")
    compile("../examples/scalajs-react-example/jvm")
    task("fastOptJS","../examples/scalajs-react-example/js")
    task("fullOptJS","../examples/scalajs-react-example/js")
    
    System.err.println(" DONE!")
    System.err.println( successes.toString ++ " succeeded, "++ failures.toString ++ " failed" )
    if(failures > 0) System.exit(1) else System.exit(0)
  }
}
