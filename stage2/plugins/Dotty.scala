package cbt
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.attribute.FileTime

trait Dotty extends BaseBuild{
  def dottyVersion: String = Dotty.version
  def dottyOptions: Seq[String] = Seq()
  override def scalaTarget: File = target ++ s"/dotty-$dottyVersion"

  def dottyCompiler: DependencyImplementation = Resolver(mavenCentral).bindOne( Dotty.compilerOnMaven( dottyVersion ) )
  def dottyLibrary: DependencyImplementation = Resolver(mavenCentral).bindOne( Dotty.libraryOnMaven( dottyVersion ) )

  // this seems needed for cbt run of dotty produced artifacts
  override def dependencies: Seq[Dependency] = Seq( dottyLibrary )

  private lazy val dottyLib = new DottyLib(
    logger, context.cbtLastModified, context.paths.mavenCache,
    context.classLoaderCache, dottyCompiler
  )

  override def compile: Option[Long] = taskCache[Dotty]("compile").memoize{
    dottyLib.compile(
      sourceFiles, compileTarget, compileStatusFile, compileDependencies, dottyOptions
    )
  }

  def doc: ExitCode =
    dottyLib.doc(
      sourceFiles, compileClasspath, docTarget, dottyOptions
    )

  override def repl = dottyLib.repl(context.args, classpath)
}

object Dotty{
  val groupId = "ch.epfl.lamp"
  val version: String = "0.1.1-20170203-da7d723-NIGHTLY"
  val libraryArtifactId = "dotty-library_2.11"
  val compilerArtifactId = "dotty-compiler_2.11"
  val interfacesArtifactId = "dotty-interfaces"
  def compilerOnMaven(version: String) = MavenDependency(groupId,compilerArtifactId,version)
  def libraryOnMaven(version: String) = MavenDependency(groupId,libraryArtifactId,version)
}

class DottyLib(
  logger: Logger,
  cbtLastModified: Long,
  mavenCache: File,
  classLoaderCache: ClassLoaderCache,
  dependency: DependencyImplementation
)(implicit transientCache: java.util.Map[AnyRef,AnyRef]){
  val lib = new Lib(logger)
  import lib._

  def repl(args: Seq[String], classpath: ClassPath) = {
    consoleOrFail("Use `cbt direct repl` instead")
    lib.runMain(
      "dotty.tools.dotc.repl.Main",
      Seq(
        "-bootclasspath",
        dependency.classpath.string,
        "-classpath",
        classpath.string
      ) ++ args,
      dependency.classLoader(classLoaderCache)
    )
  }

  def doc(
    sourceFiles: Seq[File],
    dependencyClasspath: ClassPath,
    docTarget: File,
    compileArgs: Seq[String]
  ): ExitCode = {
    if(sourceFiles.isEmpty){
      ExitCode.Success
    } else {
      docTarget.mkdirs
      val args = Seq(
        // FIXME: can we use compiler dependency here?
        "-bootclasspath", dependency.classpath.string, // FIXME: does this break for builds that don't have scalac dependencies?
        "-classpath", dependencyClasspath.string, // FIXME: does this break for builds that don't have scalac dependencies?
        "-d",  docTarget.toString
      ) ++ compileArgs ++ sourceFiles.map(_.toString)
      logger.lib("creating docs for source files "+args.mkString(", "))
      val exitCode = redirectOutToErr{
        runMain(
          "dotty.tools.dottydoc.DocDriver",
          args,
          dependency.classLoader(classLoaderCache),
          fakeInstance = true // this is a hack as Dottydoc's main method is not static
        )
      }
      System.err.println("done")
      exitCode
    }
  }

  def compile(
    sourceFiles: Seq[File],
    compileTarget: File,
    statusFile: File,
    dependencies: Seq[Dependency],
    dottyOptions: Seq[String]
  ): Option[Long] = {
    val d = Dependencies(dependencies)
    val classpath = d.classpath
    val cp = classpath.string

    if( sourceFiles.isEmpty ){
      None
    }else{
      val start = System.currentTimeMillis
      val lastCompiled = statusFile.lastModified
      if( d.lastModified > lastCompiled || sourceFiles.exists(_.lastModified > lastCompiled) ){

        val _class = "dotty.tools.dotc.Main"
        val dualArgs =
          Seq(
            "-d", compileTarget.toString
          )
        val singleArgs = dottyOptions.map( "-S" ++ _ )
        val cl = dependency.classLoader(classLoaderCache)
        val code =
          try{
            System.err.println("Compiling with Dotty to " ++ compileTarget.toString)
            compileTarget.mkdirs
            redirectOutToErr{
              lib.runMain(
                _class,
                dualArgs ++ singleArgs ++ Seq(
                  "-bootclasspath", dependency.classpath.string
                ) ++ (
                  if(cp.isEmpty) Nil else Seq("-classpath", cp) // let's put cp last. It so long
                ) ++ sourceFiles.map(_.toString),
                cl
              )
            }
          } catch {
            case e: Exception =>
            System.err.println(red("Dotty crashed. See https://github.com/lampepfl/dotty/issues. To reproduce run:"))
            System.err.println(cl)
            System.out.println(s"""
java -cp \\
${dependency.classpath.strings.mkString(":\\\n")} \\
\\
${_class} \\
\\
${dualArgs.grouped(2).map(_.mkString(" ")).mkString(" \\\n")} \\
\\
${singleArgs.mkString(" \\\n")} \\
\\
-bootclasspath \\
${dependency.classpath.strings.mkString(":\\\n")} \\
${if(cp.isEmpty) "" else ("  -classpath \\\n" ++ classpath.strings.mkString(":\\\n"))} \\
\\
${sourceFiles.sorted.mkString(" \\\n")}

"""
            )
            throw e
          }

        if(code == ExitCode.Success){
          // write version and when last compilation started so we can trigger
          // recompile if cbt version changed or newer source files are seen
          write(statusFile, "")//cbtVersion.getBytes)
          Files.setLastModifiedTime(statusFile.toPath, FileTime.fromMillis(start) )
        } else {
          System.exit(code.integer) // FIXME: let's find a better solution for error handling. Maybe a monad after all.
        }
        Some( start )
      } else {
        Some( lastCompiled )
      }
    }
  }
}
