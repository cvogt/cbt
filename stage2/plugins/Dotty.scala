package cbt
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.attribute.FileTime

trait Dotty extends BaseBuild{
  def dottyVersion: String = "0.1-20160925-b2b475d-NIGHTLY"
  def dottyOptions: Seq[String] = Seq()


  private object compileCache extends Cache[Option[File]]
  override def compile: Option[File] = compileCache{
    new DottyLib(logger).compile(
      context.cbtHasChanged,
      needsUpdate || context.parentBuild.map(_.needsUpdate).getOrElse(false),
      sourceFiles, compileTarget, compileStatusFile, dependencyClasspath ++ compileClasspath,
      context.paths.mavenCache, scalacOptions, context.classLoaderCache,
      dottyOptions = dottyOptions, dottyVersion = dottyVersion
    )
  }
}

class DottyLib(logger: Logger){
  val lib = new Lib(logger)
  import lib._

  def compile(
    cbtHasChanged: Boolean,
    needsRecompile: Boolean,
    files: Seq[File],
    compileTarget: File,
    statusFile: File,
    classpath: ClassPath,
    mavenCache: File,
    scalacOptions: Seq[String] = Seq(),
    classLoaderCache: ClassLoaderCache,
    dottyOptions: Seq[String],
    dottyVersion: String
  ): Option[File] = {

    if(classpath.files.isEmpty)
      throw new Exception("Trying to compile with empty classpath. Source files: " ++ files.toString)

    if( files.isEmpty ){
      None
    }else{
      if( needsRecompile ){
        def Resolver(urls: URL*) = MavenResolver(cbtHasChanged, mavenCache, urls: _*)
        val dotty = Resolver(mavenCentral).bindOne(
          MavenDependency("ch.epfl.lamp","dotty_2.11",dottyVersion)
        )

        val cp = classpath ++ dotty.classpath
        
        val start = System.currentTimeMillis

        val _class = "dotty.tools.dotc.Main"
        val dualArgs =
          Seq(
            "-d", compileTarget.toString
          )
        val singleArgs = dottyOptions.map( "-S" ++ _ )

        val code = 
          try{
            System.err.println("Compiling with Dotty to " ++ compileTarget.toString)
            compileTarget.mkdirs
            redirectOutToErr{
              lib.runMain(
                _class,
                dualArgs ++ singleArgs ++ Seq(
                  "-classpath", cp.string // let's put cp last. It so long
                ) ++ files.map(_.toString),
                dotty.classLoader(classLoaderCache)
              )
            }
          } catch {
            case e: Exception =>
            System.err.println(red("Dotty crashed. Try running it by hand:"))
            System.out.println(s"""
java -cp \\
${dotty.classpath.strings.mkString(":\\\n")} \\
\\
${_class} \\
\\
${dualArgs.grouped(2).map(_.mkString(" ")).mkString(" \\\n")} \\
\\
${singleArgs.mkString(" \\\n")} \\
\\
-classpath \\
${cp.strings.mkString(":\\\n")} \\
\\
${files.sorted.mkString(" \\\n")}
"""
            )
            ExitCode.Failure
          }

        if(code == ExitCode.Success){
          // write version and when last compilation started so we can trigger
          // recompile if cbt version changed or newer source files are seen
          write(statusFile, "")//cbtVersion.getBytes)
          Files.setLastModifiedTime(statusFile.toPath, FileTime.fromMillis(start) )
        } else {
          System.exit(code.integer) // FIXME: let's find a better solution for error handling. Maybe a monad after all.
        }
      }
      Some( compileTarget )
    }
  }
}
