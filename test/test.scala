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
  def cbtMain(context: Context): ExitCode = {
    import context._
    val _args = context.args
    val args = new Stage1ArgsParser(_args.toVector)
    implicit val logger: Logger = new Logger(args.enabledLoggers, System.currentTimeMillis)
    val lib = new Lib(logger)
    val mavenCache = cache ++ "/maven"

    val slow = (
      System.getenv("CIRCLECI") != null // enable only on circle
      || args.args.contains("slow")
    )
    val compat = !args.args.contains("no-compat")
    val shellcheck = !args.args.contains("no-shellcheck")
    val fork = args.args.contains("fork")
    val direct = args.args.contains("direct")

    if(!slow) System.err.println( "Skipping slow tests" )
    if(!compat) System.err.println( "Skipping cbt version compatibility tests" )
    if(fork) System.err.println( "Forking tests" )
    if(direct) System.err.println( "Running tests in direct mode" )

    if(shellcheck){
      val pb = new ProcessBuilder( "/usr/bin/env", "shellcheck", (cbtHome / "cbt").string )
      val p = pb.start
      val out = new java.io.InputStreamReader(p.getInputStream)
      val errors = Iterator.continually(out.read).takeWhile(_ != -1).map(_.toChar).mkString
      if( p.waitFor != 0 ){
        throw new Exception("Linting error in ./cbt bash launcher script:\n" + errors)
      }
    } else System.err.println( "Skipping shellcheck" )

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
      val workingDirectory = cbtHome / "test" / path
      if( fork ){
        val allArgs = Seq((cbtHome / "cbt").string) ++ (if(direct) Seq("direct") else Nil) ++ _args ++ args.propsRaw
        logger.test(allArgs.toString)
        val pb = new ProcessBuilder( allArgs :_* )
        pb.directory( workingDirectory )
        val p = pb.start
        val serr = new InputStreamReader(p.getErrorStream);
        val sout = new InputStreamReader(p.getInputStream);
        import scala.concurrent.ExecutionContext.Implicits.global
        val err = Future(blocking(Iterator.continually(serr.read).takeWhile(_ != -1).map(_.toChar).mkString))
        val out = Future(blocking(Iterator.continually(sout.read).takeWhile(_ != -1).map(_.toChar).mkString))
        p.waitFor
        p.exitValue
        Result(
          p.exitValue === 0,
          Await.result( out, Duration.Inf ),
          Await.result( err, Duration.Inf )
        )
      } else {
        val c = context.copy(
          workingDirectory = workingDirectory,
          args = _args.drop(1),
          transientCache = new java.util.HashMap()
        )
        val ( outVar, errVar, _ ) = lib.getOutErrIn
        val oldOut = outVar.get
        val oldErr = errVar.get
        val out = new ByteArrayOutputStream
        val err = new ByteArrayOutputStream
        val out2 = new ByteArrayOutputStream
        val err2 = new ByteArrayOutputStream
        try{
          outVar.set(new PrintStream(out))
          errVar.set(new PrintStream(err))
          val exitValue = try{
            scala.Console.withOut(out2)(
              scala.Console.withErr(err2)(
                lib.trapExitCode(
                  lib.callReflective( DirectoryDependency(c,None), _args.headOption, c )
                )
              )
            )
          } catch {
            case scala.util.control.NonFatal(e) =>
              lib.redirectOutToErr( e.printStackTrace )
              ExitCode.Failure
          }
          System.out.flush
          System.err.flush
          Result(
            exitValue.integer === 0,
            out.toString ++ out2.toString,
            err.toString ++ err2.toString
          )
        } finally {
          outVar.set(oldOut)
          errVar.set(oldErr)
        }
      }
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
    def run(path: String)(implicit logger: Logger) = task("run", path)
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

    implicit val transientCache: java.util.Map[AnyRef,AnyRef] = new java.util.HashMap
    implicit val classLoaderCache: ClassLoaderCache = new ClassLoaderCache( new java.util.HashMap )
    def Resolver(urls: URL*) = MavenResolver(cbtLastModified, mavenCache, urls: _*)

    {
      val b = new BasicBuild(context){
        override def dependencies =
          Resolver(mavenCentral).bind(
            MavenDependency("net.incongru.watchservice","barbary-watchservice","1.0"),
            MavenDependency("net.incongru.watchservice","barbary-watchservice","1.0")
          )
      }
      val cp = b.classpath
      assert(cp.strings.distinct == cp.strings, "duplicates in classpath: " ++ cp.string)
    }

    {
      def d = Resolver(mavenCentral).bindOne(
        MavenDependency("net.incongru.watchservice","barbary-watchservice","1.0")
      )
      assert(d === d)
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
        Resolver( bintray("tpolecat"), mavenCentral ).bind(
          lib.ScalaDependency("org.tpolecat","tut-core","0.4.2", scalaMajorVersion="2.11", verifyHash = false)
        )
      ).classpath.strings
      ++
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
    run("simple")
    clean("simple")
    if( compat ){
      usage("simple-fixed")
      compile("simple-fixed")
    }

    compile("../plugins/sbt_layout")
    compile("../plugins/scalafmt")
    compile("../plugins/scalajs")
    compile("../plugins/scalariform")
    compile("../plugins/wartremover")
    compile("../plugins/uber-jar")
    compile("../plugins/scalafix-compiler-plugin")
    compile("../examples/fork-example")
    compile("../examples/scalafmt-example")
    compile("../examples/scalariform-example")
    compile("../examples/scalatest-example")
    compile("../plugins/scalastyle")
    if(slow){
      compile("../examples/scalajs-react-example/js")
      compile("../examples/scalajs-react-example/jvm")
      compile("../examples/scalajs-plain-example/js")
      compile("../examples/scalajs-plain-example/jvm")
    }
    compile("../examples/multi-standalone-example")
    compile("../examples/multi-combined-example")
    if(sys.props("java.version").startsWith("1.7")){
      System.err.println("\nskipping dotty tests on Java 7")
    } else {
      compile("../examples/dotty-example")
      task("run","../examples/dotty-example")
      if(slow){
        task("dottydoc","../examples/dotty-example")
      }
    }
    if(slow){
      task("compile","../examples/scalajs-react-example/js")
      task("fullOpt.compile","../examples/scalajs-react-example/js")
    }
    compile("../examples/uber-jar-example")

    if( compat && fork ){ // FIXME: this should not be excluded in forking
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
      val res = runCbt("../examples/scalastyle", Seq("scalastyle"))
      assert(!res.exit0)
      assert(res.err contains "Line contains a tab", res.out ++ "\n" ++ res.err)
      assert(res.out.isEmpty, res.out ++ "\n" ++ res.err)
    }

    {
      val res = runCbt("../examples/scalapb-example", Seq("run"))
      assert(res.exit0)
      assert(res.out contains "age: 123", res.out ++ "\n--\n" ++ res.err)
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
      assert(res.out == "2\n", res.out ++ "\n--\n" ++ res.err)
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
      assert(res.out == "2.10.5\n2.11.7\n", res.out)
    }

    {
      val res = runCbt("../libraries/eval", Seq("test.run"))
      assert(res.exit0)
      assert(res.out.contains("All tests passed"), res.out)
    }

    {
      val res = runCbt("../examples/new-style-macros-example", Seq("run"))
      assert(res.exit0)
      assert(res.out.contains("hello, world!"), res.out)
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

    {
      val res = runCbt("../examples/cross-rewrite-example", Seq("cross.exportedClasspath"))
      assert(res.exit0)
      Seq("cats","scalaz","2.11.8","2.12.1").foreach(
        s => assert(res.out contains s, res.out)
      )
    }

    {
      val sourceFile = cbtHome / "examples" / "scalafix-compiler-plugin-example" / "Main.scala"
      val sourceBefore = sourceFile.readAsString
      runCbt("../examples/scalafix-compiler-plugin-example", Seq("clean","force"))
      val res = runCbt("../examples/scalafix-compiler-plugin-example", Seq("compile"))
      assert(res.exit0)
      val sourceAfter = sourceFile.readAsString
      assert(!(sourceBefore contains "@volatile"))
      assert(!(sourceBefore contains ": Unit"))
      assert(!(sourceBefore contains ": String "))
      assert(!(sourceBefore contains "import scala.collection.immutable"))
      assert(sourceAfter contains "@volatile")
      assert(sourceAfter contains ": Unit")
      assert(sourceAfter contains ": String ")
      assert(sourceAfter contains "import scala.collection.immutable")
      lib.write(sourceFile, sourceBefore)
    }

    /*
    // currently fails with
    // java.lang.UnsupportedOperationException: scalafix.rewrite.ScalafixMirror.fromMirror $anon#typeSignature requires the semantic api
    {
      val sourceFile = cbtHome / "examples" / "scalafix-example" / "Main.scala"
      val sourceBefore = sourceFile.readAsString
      runCbt("../examples/scalafix-example", Seq("clean","force"))
      val res = runCbt("../examples/scalafix-example", Seq("compile"))
      assert(res.exit0)
      val sourceAfter = sourceFile.readAsString
      assert(!(sourceBefore contains "@volatile"))
      assert(!(sourceBefore contains ": Unit"))
      assert(!(sourceBefore contains ": String "))
      assert(!(sourceBefore contains "import scala.collection.immutable"))
      assert(sourceAfter contains "@volatile")
      assert(sourceAfter contains ": Unit")
      assert(sourceAfter contains ": String ")
      assert(sourceAfter contains "import scala.collection.immutable")
      lib.write(sourceFile, sourceBefore)
    }
    */

    System.err.println(" DONE!")
    System.err.println( successes.toString ++ " succeeded, "++ failures.toString ++ " failed" )
    if(failures > 0) ExitCode.Failure else ExitCode.Success
  }
}
