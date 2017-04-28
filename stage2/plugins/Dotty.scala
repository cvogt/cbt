package cbt
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.attribute.FileTime

trait Dotty extends CustomDotty{
  def dottyVersion: String = Dotty.version
  def dottyCompiler: DependencyImplementation = Resolver(mavenCentral).bindOne( Dotty.compilerOnMaven( dottyVersion ) )
  def dottyLibrary: DependencyImplementation = Resolver(mavenCentral).bindOne( Dotty.libraryOnMaven( dottyVersion ) )
  override def scalaTarget: File = target ++ s"/dotty-$dottyVersion"

  // this seems needed for cbt run of dotty produced artifacts
  override def dependencies: Seq[Dependency] = Seq( dottyCompiler )
}
trait CustomDotty extends BaseBuild{
  def dottyOptions: Seq[String] = Seq()
  override def scalaTarget: File = target ++ s"/dotty"

  def dottyCompiler: DependencyImplementation

  private lazy val dottyLib = new DottyLib(
    context.cbtLastModified, context.paths.mavenCache, dottyCompiler
  )

  def compileJavaFirst: Boolean = false

  override def dependencies: Seq[Dependency] = Seq()

  // this makes sure the scala or java classes compiled first are available on subsequent compile
  //override def compileDependencies: Seq[Dependency]
  //  = super.compileDependencies //++ Seq( compileTarget ).filter(_.exists).map( t => BinaryDependency( Seq(t), Nil ) )

  override def compile: Option[Long] = taskCache[Dotty]("compile").memoize{
    def compileDotty =
      dottyLib.compile(
        sourceFiles.filter(f => f.string.endsWith(".scala") || f.string.endsWith(".java")),
        compileTarget, compileStatusFile, compileDependencies, dottyOptions
      )

    def compileJava =
      lib.compile(
        context.cbtLastModified,
        sourceFiles.filter(_.string.endsWith(".java")),
        compileTarget, compileStatusFile, compileDependencies, context.paths.mavenCache,
        scalacOptions, zincVersion = zincVersion, scalaVersion = scalaVersion
      )

    def set(time: Long) = if(compileStatusFile.exists) Files.setLastModifiedTime(compileStatusFile.toPath, FileTime.fromMillis(time) )

    val before = compileStatusFile.lastModified
    val firstLastModified = if(compileJavaFirst) compileJava else compileDotty
    set( before )
    val secondLastModified = if(!compileJavaFirst) compileJava else compileDotty
    val min = (firstLastModified ++ secondLastModified).reduceOption(_ min _).getOrElse(0l)
    set( min )
    Some(min)
  }

  def dottydoc: ExitCode =
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
  cbtLastModified: Long,
  mavenCache: File,
  dottyCompiler: DependencyImplementation
)(implicit transientCache: java.util.Map[AnyRef,AnyRef], classLoaderCache: ClassLoaderCache, logger: Logger){
  val lib = new Lib(logger)
  import lib._

  def repl(args: Seq[String], classpath: ClassPath) = {
    consoleOrFail("Use `cbt direct repl` instead")
    dottyCompiler.runMain(
      "dotty.tools.dotc.repl.Main",
      Seq(
        "-bootclasspath",
        dottyCompiler.classpath.string,
        "-classpath",
        classpath.string
      ) ++ args
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
        "-bootclasspath", dottyCompiler.classpath.string, // FIXME: does this break for builds that don't have scalac dependencies?
        "-classpath", dependencyClasspath.string, // FIXME: does this break for builds that don't have scalac dependencies?
        "-d",  docTarget.toString
      ) ++ compileArgs ++ sourceFiles.map(_.toString)
      logger.lib("creating docs for source files "+args.mkString(", "))
      val exitCode = redirectOutToErr{
        dottyCompiler.runMain(
          "dotty.tools.dottydoc.DocDriver",
          args
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
    val d = Dependencies( dependencies )
    val classpath = d.classpath
    val cp = classpath.string

    def lastModified = (
      cbtLastModified +: d.lastModified +: sourceFiles.map(_.lastModified)
    ).max

    if( sourceFiles.isEmpty ){
      None
    }else{
      val start = System.currentTimeMillis
      val lastCompiled = statusFile.lastModified
      if( lastModified > lastCompiled ){

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
              println(
                s"""
------------------------------
java -cp \\
${dottyCompiler.classpath.strings.mkString(":\\\n")} \\
\\
${_class} \\
\\
${dualArgs.grouped(2).map(_.mkString(" ")).mkString(" \\\n")} \\
\\
${singleArgs.mkString(" \\\n")} \\
\\
${if(cp.isEmpty) "" else Seq("-classpath", cp).mkString("\n\n")}
\\
${sourceFiles.sorted.mkString(" \\\n")}
------------------------------
"""
              )

              dottyCompiler.runMain(
                _class,
                dualArgs ++ singleArgs ++ /* Seq(
                  "-bootclasspath", dottyCompiler.exportedClasspath.string
                ) ++*/ (
                  if(cp.isEmpty) Nil else Seq("-classpath", cp)
                    //":/Users/fixel/Projects/dotty/library/target/hack") // let's put cp last. It so long
                ) ++ Seq(
                  "-sourcepath", "/Users/fixel/Projects/dotty/library/src"
                ) ++ sourceFiles.map(_.toString)
              )
            }
          } catch {
            case e: Exception =>
            System.err.println(red("Dotty crashed. See https://github.com/lampepfl/dotty/issues. To reproduce run:"))
            System.err.println(dottyCompiler.classLoader)
            System.out.println(s"""
java -cp \\
${dottyCompiler.classpath.strings.mkString(":\\\n")} \\
\\
${_class} \\
\\
${dualArgs.grouped(2).map(_.mkString(" ")).mkString(" \\\n")} \\
\\
${singleArgs.mkString(" \\\n")} \\
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
