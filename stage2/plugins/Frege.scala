package cbt
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.attribute.FileTime

trait Frege extends BaseBuild{
  def fregeVersion: String = "3.24.100.1"
  def classifier: Option[String] = Some("jdk8")
  def fregeTarget: String = "1.8"
  def enableMakeMode = true
  def enableOptimisation = true
  def fregeDependencies: Seq[Dependency] = dependencies
  def inline = true

  private def fregeOptions: Seq[String] = {
    val opts : Seq[(String, Boolean)] = Seq(("-make", enableMakeMode), ("-O", enableOptimisation), ("-inline", inline) )
    opts.filter(_._2).map(_._1)
  }
  override def scalaTarget: File = target ++ s"/frege-$fregeVersion"

  private lazy val fregeLib = new FregeLib(
    context.cbtLastModified, context.paths.mavenCache,
    fregeVersion = fregeVersion, classifier = classifier,
    fregeDependencies = fregeDependencies, fregeTarget = fregeTarget
  )

  override def sourceFileFilter(file: File): Boolean = file.toString.endsWith(".fr") || file.toString.endsWith(".java")

  override def compile: Option[Long] = taskCache[Frege]("compile").memoize{
    fregeLib.compile(
      sourceFiles, compileTarget, compileStatusFile, dependencies, fregeOptions
    )
  }

  override def dependencies = Resolver(mavenCentral).bind(
    MavenDependency("org.frege-lang","frege",fregeVersion, Classifier(classifier))
  )

}

class FregeLib(
  cbtLastModified: Long,
  mavenCache: File,
  fregeVersion: String,
  classifier: Option[String],
  fregeDependencies: Seq[Dependency],
  fregeTarget: String
)(implicit transientCache: java.util.Map[AnyRef,AnyRef], classLoaderCache: ClassLoaderCache, logger: Logger){
  val lib = new Lib(logger)
  import lib._

  private def Resolver(urls: URL*) = MavenResolver(cbtLastModified, mavenCache, urls: _*)
  private lazy val fregeDependency = Resolver(mavenCentral).bindOne(
    MavenDependency("org.frege-lang","frege",fregeVersion, Classifier(classifier))
  )

  def compile(
    sourceFiles: Seq[File],
    compileTarget: File,
    statusFile: File,
    dependencies: Seq[Dependency],
    fregeOptions: Seq[String]
  )(implicit classLoaderCache: ClassLoaderCache): Option[Long] = {
    val d = Dependencies(dependencies)
    val classpath = d.classpath
    val cp = classpath.string

    if( sourceFiles.isEmpty ){
      None
    } else {
      val start = System.currentTimeMillis
      val lastCompiled = statusFile.lastModified
      if( d.lastModified > lastCompiled || sourceFiles.exists(_.lastModified > lastCompiled) ){

        val _class = "frege.compiler.Main"
        val fp = (fregeDependency.classpath.strings ++ fregeDependencies.map(_.classpath.string))
        val dualArgs =
          Seq(
            "-target", fregeTarget,
            "-d", compileTarget.toString
          ) ++ (
            if(fp.isEmpty) Nil else Seq("-fp", fp.mkString(":"))
          )
        val singleArgs = fregeOptions
        val code = 
          try{
            System.err.println("Compiling with Frege to " ++ compileTarget.toString)
            compileTarget.mkdirs
            redirectOutToErr{
              lib.runMain(
                _class,
                dualArgs ++ singleArgs ++ sourceFiles.map(_.toString),
                fregeDependency.classLoader
              )
            }
          } catch {
            case e: Exception =>
            System.err.println(red("Frege crashed. To reproduce run:"))
            System.out.println(s"""
java -cp \\
${fregeDependency.classpath.strings.mkString(":\\\n")} \\
\\
${_class} \\
\\
${dualArgs.grouped(2).map(_.mkString(" ")).mkString(" \\\n")} \\
\\
${singleArgs.mkString(" \\\n")} \\
\\
-bootclasspath \\
${fregeDependency.classpath.strings.mkString(":\\\n")} \\
${if(classpath.strings.isEmpty) "" else ("  -fp \\\n" ++ classpath.strings.mkString(":\\\n"))} \\
\\
${sourceFiles.sorted.mkString(" \\\n")}
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
        Some( start )
      } else {
        Some( lastCompiled )
      }
    }
  }
}
