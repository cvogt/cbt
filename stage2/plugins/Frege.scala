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
  def fregeDependencies: Seq[String] = dependencies.map{ _.classpath.string}
  def inline = true

  private def fregeOptions: Seq[String] = {
    val opts : Seq[(String, Boolean)] = Seq(("-make", enableMakeMode), ("-O", enableOptimisation), ("-inline", inline) )
    opts.filter(_._2).map(_._1)
  }
  override def scalaTarget: File = target ++ s"/frege-$fregeVersion"
  
  private lazy val fregeLib = new FregeLib(
    logger, context.cbtHasChanged, context.paths.mavenCache,
    context.classLoaderCache, fregeVersion = fregeVersion, classifier = classifier,
    fregeDependencies = fregeDependencies, fregeTarget = fregeTarget
  )

  override def sourceFileFilter(file: File): Boolean = file.toString.endsWith(".fr") || file.toString.endsWith(".java")

  private object compileCache extends Cache[Option[File]]
  override def compile: Option[File] = compileCache{
    fregeLib.compile(
      needsUpdate || context.parentBuild.map(_.needsUpdate).getOrElse(false),
      sourceFiles, compileTarget, compileStatusFile, compileClasspath,
      fregeOptions
    )
  }

  override def dependencies = Resolver(mavenCentral).bind(
    MavenDependency("org.frege-lang","frege",fregeVersion, Classifier(classifier))
  )

}

class FregeLib(
  logger: Logger,
  cbtHasChanged: Boolean,
  mavenCache: File,
  classLoaderCache: ClassLoaderCache,
  fregeVersion: String,
  classifier: Option[String],
  fregeDependencies: Seq[String],
  fregeTarget: String
){
  val lib = new Lib(logger)
  import lib._

  private def Resolver(urls: URL*) = MavenResolver(cbtHasChanged, mavenCache, urls: _*)
  private lazy val fregeDependency = Resolver(mavenCentral).bindOne(
    MavenDependency("org.frege-lang","frege",fregeVersion, Classifier(classifier))
  )

  def compile(
    needsRecompile: Boolean,
    files: Seq[File],
    compileTarget: File,
    statusFile: File,
    classpath: ClassPath,
    fregeOptions: Seq[String]
  ): Option[File] = {

    if(classpath.files.isEmpty)
      throw new Exception("Trying to compile with empty classpath. Source files: " ++ files.toString)

    if( files.isEmpty ){
      None
    }else{
      if( needsRecompile ){
        val start = System.currentTimeMillis

        val _class = "frege.compiler.Main"
        val dualArgs =
          Seq(
            "-target", fregeTarget,
            "-d", compileTarget.toString,
            "-fp", (fregeDependency.classpath.strings ++ fregeDependencies).mkString(":")
          )
        val singleArgs = fregeOptions
        val code = 
          try{
            System.err.println("Compiling with Frege to " ++ compileTarget.toString)
            compileTarget.mkdirs
            redirectOutToErr{
              lib.runMain(
                _class,
                dualArgs ++ singleArgs ++ files.map(_.toString),
                fregeDependency.classLoader(classLoaderCache)
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
-classpath \\
${classpath.strings.mkString(":\\\n")} \\
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
