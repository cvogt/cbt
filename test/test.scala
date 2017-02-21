package cbt
package test
import java.io.File
import java.nio.file._
import java.net.URL
import java.util.{Iterator=>_,_}
import scala.concurrent._
import scala.concurrent.duration._
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
      val serr = new InputStreamReader(p.getErrorStream);
      val sout = new InputStreamReader(p.getInputStream);
      import scala.concurrent.ExecutionContext.Implicits.global
      val err = Future(blocking(Iterator.continually(serr.read).takeWhile(_ != -1).map(_.toChar).mkString))
      val out = Future(blocking(Iterator.continually(sout.read).takeWhile(_ != -1).map(_.toChar).mkString))
      p.waitFor
      Result(
        p.exitValue == 0,
        Await.result( out, Duration.Inf ),
        Await.result( err, Duration.Inf )
      )
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
      //assert(res.out == "", res.err.toString)
      assert(res.out contains usageString, usageString + " not found in " ++ res.toString)
    }
    def compile(path: String)(implicit logger: Logger) = task("compile", path)
    def task(name: String, path: String)(implicit logger: Logger) = {
      val res = runCbt(path, Seq(name))
      val debugToken = name ++ " " ++ path ++ " "
      assertSuccess(res,debugToken)
      res
      // assert(res.err == "", res.err) // FIXME: enable this
    }

    def clean(path: String)(implicit logger: Logger) = {
      val res = runCbt(path, Seq("clean", "dry-run", "force"))
      val debugToken = "\n"++lib.red("Deleting") ++ " " ++ (cbtHome ++("/test/"++path++"/target")).toPath.toAbsolutePath.toString++"\n"
      val debugToken2 = "\n"++lib.red("Deleting") ++ " " ++ (cbtHome ++("/test/"++path)).toPath.toAbsolutePath.toString++"\n"
      assertSuccess(res,debugToken)
      assert(res.out == "", "should be empty: " + res.out)
      assert(res.err.contains(debugToken), debugToken ++ " missing from " ++ res.err.toString)
      assert(
        !res.err.contains(debugToken2),
        "Tried to delete too much: " ++ debugToken2 ++ " found in " ++ res.err.toString
      )
      res.err.split("\n").filter(_.startsWith(lib.red("Deleting"))).foreach{ line =>
        assert(
          line.size >= debugToken2.trim.size,
          "Tried to delete too much: " ++ line ++"   debugToken2: " ++ debugToken2
        )
      }
    }

    logger.test( "Running tests " ++ _args.toList.toString )

    val cache = cbtHome ++ "/cache"
    val mavenCache = cache ++ "/maven"
    val cbtLastModified = System.currentTimeMillis
    implicit val transientCache: java.util.Map[AnyRef,AnyRef] = new java.util.HashMap
    implicit val classLoaderCache: ClassLoaderCache = new ClassLoaderCache( new java.util.HashMap )
    def Resolver(urls: URL*) = MavenResolver(cbtLastModified, mavenCache, urls: _*)

    {
      val noContext = new ContextImplementation(
        cbtHome ++ "/test/nothing",
        cbtHome,
        Array(),
        Array(),
        start,
        cbtLastModified,
        null,
        new HashMap[AnyRef,AnyRef],
        new HashMap[AnyRef,AnyRef],
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
      (
        if(System.getenv("CIRCLECI") == null){
          // tenporarily disable on circleci as it seems to have trouble reliably
          // downloading from bintray
          Dependencies(
            Resolver( bintray("tpolecat") ).bind(
              lib.ScalaDependency("org.tpolecat","tut-core","0.4.2", scalaMajorVersion="2.11")
            )
          ).classpath.strings
        } else Nil
      ) ++
     Dependencies(
      Resolver( sonatypeReleases ).bind(
        MavenDependency("org.cvogt","scala-extensions_2.11","0.5.1")
      )
    ).classpath.strings
      ++
      Dependencies(
        Resolver( mavenCentral ).bind(
          MavenDependency("ai.x","lens_2.11","1.0.0")
        )
      ).classpath.strings
    ).foreach{
      path => assert(new File(path).exists, path)
    }

    ScaffoldTest.main(Array())

    usage("nothing")
    compile("nothing")
    //clean("nothing")
    usage("multi-build")
    compile("multi-build")
    clean("multi-build")
    usage("simple")
    compile("simple")
    clean("simple")
    usage("simple-fixed")
    compile("simple-fixed")
    
    compile("../plugins/sbt_layout")
    compile("../plugins/scalafmt")
    compile("../plugins/scalajs")
    compile("../plugins/scalariform")
    compile("../plugins/scalatest")
    compile("../plugins/wartremover")
    compile("../plugins/uber-jar")
    compile("../examples/scalafmt-example")
    compile("../examples/scalariform-example")
    compile("../examples/scalatest-example")
    compile("../examples/scalajs-react-example/js")
    compile("../examples/scalajs-react-example/jvm")
    compile("../examples/multi-standalone-example")
    compile("../examples/multi-combined-example")
    if(sys.props("java.version").startsWith("1.7")){
      System.err.println("\nskipping dotty tests on Java 7")
    } else {
      compile("../examples/dotty-example")
      task("run","../examples/dotty-example")
      task("doc","../examples/dotty-example")
    }
    task("compile","../examples/scalajs-react-example/js")
    task("fullOpt.compile","../examples/scalajs-react-example/js")
    compile("../examples/uber-jar-example")
    
    {
      val res = task("docJar","simple-fixed-cbt")
      assert( res.out endsWith "simple-fixed-cbt_2.11-0.1-javadoc.jar\n", res.out )
      assert( res.err contains "model contains", res.err )
      assert( res.err endsWith "documentable templates\n", res.err )
    }
    
    {
      val res = runCbt("simple", Seq("printArgs","1","2","3"))
      assert(res.exit0)
      assert(res.out == "1 2 3\n", res.out)
    }

    {
      val res = runCbt("../examples/build-info-example", Seq("run"))
      assert(res.exit0)
      assert(res.out contains "version: 0.1", res.out)
    }

    {
      val res = runCbt("broken-build/build-class-with-wrong-arguments", Seq("run"))
      assert(!res.exit0)
      assert(res.err contains s"Expected class ${lib.buildClassName}(val context: Context), but found different constructor", res.err)
      assert(res.err contains s"${lib.buildClassName}(int, interface cbt.Context)", res.err)
    }

    {
      val res = runCbt("broken-build/build-class-with-wrong-parent", Seq("run"))
      assert(!res.exit0)
      assert(res.err contains s"You need to define a class ${lib.buildClassName} extending an appropriate super class", res.err)
    }

    {
      val res = runCbt("broken-build/no-build-file", Seq("run"))
      assert(!res.exit0)
      assert(res.err contains s"No file ${lib.buildFileName} (lower case) found", res.err)
    }

    {
      val res = runCbt("broken-build/empty-build-file", Seq("run"))
      assert(!res.exit0)
      assert(res.err contains s"You need to define a class ${lib.buildClassName}", res.err)
    }

    {
      val res = runCbt("../examples/wartremover-example", Seq("compile"))
      assert(!res.exit0)
      assert(res.err.contains("var is disabled"), res.err)
      assert(res.err.contains("null is disabled"), res.err)
    }

    {
      val res = runCbt("../examples/dynamic-overrides-example", Seq("with","""def dummy = "1.2.3" """, "dummy"))
      assert(res.exit0)
      assert(res.out == "1.2.3\n", res.out ++ res.err)
    }

    {
      val res = runCbt("../examples/dynamic-overrides-example", Seq("with","""def dummy = "1.2.3" """, "dummy"))
      assert(res.exit0)
      assert(res.out == "1.2.3\n", res.out ++ res.err)
    }

    {
      val res = runCbt("../examples/dynamic-overrides-example", Seq("eval",""" scalaVersion; 1 + 1 """))
      assert(res.exit0)
      assert(res.out == "2\n", res.out ++ res.err)
    }

    {
      val res = runCbt("../examples/dynamic-overrides-example", Seq("foo"))
      assert(res.exit0)
      assert(res.out == "Build\n", res.out ++ res.err)
    }

    {
      val res = runCbt("../examples/dynamic-overrides-example", Seq("bar"))
      assert(res.exit0)
      assert(res.out startsWith "Bar: DynamicBuild", res.out)
      assert(res.out startsWith "", res.out ++ res.err)
    }

    {
      val res = runCbt("../examples/dynamic-overrides-example", Seq("baz"))
      assert(res.exit0)
      assert(res.out startsWith "Bar: DynamicBuild", res.out ++ res.err)
    }

    {
      val res = runCbt("../examples/dynamic-overrides-example", Seq("bam"))
      assert(res.exit0)
      assert(res.out startsWith "Baz: DynamicBuild", res.out ++ res.err)
    }

    {
      val res = runCbt("../examples/dynamic-overrides-example", Seq("foo2"))
      assert(res.exit0)
      assert(res.out == "Build\n", res.out ++ res.err)
    }

    {
      val res = runCbt("../examples/dynamic-overrides-example", Seq("bar2"))
      assert(res.exit0)
      assert(res.out startsWith "Bar2: Some(DynamicBuild", res.out ++ res.err)
    }

    {
      val res = runCbt("../examples/dynamic-overrides-example", Seq("baz2"))
      assert(res.exit0)
      assert(res.out startsWith "Bar2: Some(DynamicBuild", res.out ++ res.err)
    }
    {
      val res = runCbt("../examples/cross-build-example", Seq("cross.scalaVersion"))
      assert(res.exit0)
      assert(res.out == "2.10.5\n2.11.7\n", res.out ++ res.err)
    }

    {
      val res = runCbt("../libraries/eval", Seq("test.run"))
      assert(res.exit0)
      assert(res.out.contains("All tests passed"), res.out)
    }

    {
      val res = runCbt("../examples/resources-example", Seq("run"))
      assert(res.exit0)
      assert(res.out.contains("via parent.parent: false 0"), res.out)
    }

    {
      val res = runCbt("../examples/resources-example", Seq("runFlat"))
      assert(res.exit0)
      assert(res.out.contains("via parent.parent: true 2"), res.out)
    }

    System.err.println(" DONE!")
    System.err.println( successes.toString ++ " succeeded, "++ failures.toString ++ " failed" )
    if(failures > 0) System.exit(1) else System.exit(0)
  }
}
