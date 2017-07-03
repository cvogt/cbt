package cbt

import java.io.{File, FileNotFoundException}
import java.lang.management.ManagementFactory
import java.util.{Map => JMap}
import scala.collection.JavaConverters._
import scala.sys.process._
import scala.util.Try
import scala.util.control.NonFatal

/**
  * Plugin that provides ENSIME (http://ensime.org/) support.
  *
  * The central method provided by this plugin is `ensime`, which will generate an ENSIME
  * configuration file describing the current build. This configuration file may be used by ENSIME
  * server and clients, adding IDE capabilities to plain text editors.
  *
  * Most methods and helper classes were copied verbatim or with minor changes from the
  * ensime-related projects, ensime-server (https://github.com/ensime/ensime-server) and ensime-sbt
  * (https://github.com/ensime/ensime-sbt). Licensed under the Apache 2.0 License.
  */
trait Ensime extends BaseBuild {

  /** The version of ensime to use. */
  def ensimeServerVersion: String = "2.0.0-SNAPSHOT"

  /** ENSIME server and client configuration. */
  def ensimeConfig: Ensime.EnsimeConfig = Ensime.EnsimeConfig(
    scalaCompilerJars = {
      new ScalaDependencies(
        context.cbtLastModified,
        context.paths.mavenCache,
        scalaVersion
      ).dependencies.flatMap(_.exportedClasspathArray)
    },
    ensimeServerJars = {
      val deps = Dependencies(Resolver(mavenCentral, sonatypeReleases, sonatypeSnapshots).bind(
        MavenDependency("org.ensime", s"server_$scalaMajorVersion", ensimeServerVersion)
      ))
      (deps.dependencies ++ deps.transitiveDependencies).flatMap(_.exportedClasspathArray)
    },
    ensimeServerVersion = scalaVersion,
    rootDir = projectDirectory,
    cacheDir = projectDirectory / ".ensime_cache",
    javaHome = Ensime.jdkHome,
    name = name,
    scalaVersion = scalaVersion,
    javaSources = Ensime.jdkSource.toSeq,
    javaFlags = Ensime.baseJavaFlags(ensimeServerVersion),
    projects = Ensime.findProjects(this)
  )

  /** Generate an ENSIME configuration file for this build. */
  def ensime: File = lib.writeIfChanged(
    projectDirectory / ".ensime",
    ensimeConfig.toSexp
  )

}

object Ensime {

  def jdkHome: File = List(
    // manual
    sys.env.get("JDK_HOME"),
    sys.env.get("JAVA_HOME"),
    // fallback
    sys.props.get("java.home").map(new File(_).getParent),
    sys.props.get("java.home"),
    // osx
    Try("/usr/libexec/java_home".!!.trim).toOption
  ).flatten.filter { n =>
    new File(n + "/lib/tools.jar").exists
  }.headOption.map(new File(_).getCanonicalFile).getOrElse(
    throw new FileNotFoundException(
      """Could not automatically find the JDK/lib/tools.jar.
      |You must explicitly set JDK_HOME or JAVA_HOME.""".stripMargin
    )
  )

  def jdkSource: Option[File] = {
    val src = new File(jdkHome, "src.zip")
    if (src.exists) Some(src) else None
  }

  def baseJavaFlags(serverVersion: String): Seq[String] = {
    val raw = ManagementFactory.getRuntimeMXBean.getInputArguments.asScala.toList

    // WORKAROUND https://github.com/ensime/ensime-sbt/issues/91
    // WORKAROUND https://github.com/ensime/ensime-server/issues/1756
    val StackSize = "-Xss[^ ]+".r
    val MinHeap = "-Xms[^ ]+".r
    val MaxHeap = "-Xmx[^ ]+".r
    val MaxPerm = "-XX:MaxPermSize=[^ ]+".r
    val corrected = raw.filter {
      case StackSize() => false
      case MinHeap()   => false
      case MaxHeap()   => false
      case MaxPerm()   => false
      case other       => true
    }
    val memory = Seq(
      "-Xss2m",
      "-Xms512m",
      "-Xmx4g"
    )

    val server = serverVersion.substring(0, 3)
    val java = sys.props("java.version").substring(0, 3)
    val versioned = (java, server) match {
      case (_, "1.0") | ("1.6" | "1.7", _) => Seq(
        "-XX:MaxPermSize=256m"
      )
      case _ => List(
        "-XX:MaxMetaspaceSize=256m",
        // these improve ensime-server performance
        "-XX:StringTableSize=1000003",
        "-XX:+UnlockExperimentalVMOptions",
        "-XX:SymbolTableSize=1000003"
      )
    }

    // WORKAROUND: https://github.com/scala/scala/pull/5592
    val zipFix = Seq("-Dscala.classpath.closeZip=true")

    corrected ++ memory ++ versioned ++ zipFix
  }

  /** Attempt to download a maven dependency with a given classifer. Warn if it is not available. */
  def resolveWithClassifier(dependencies: Seq[Dependency], classifier: Classifier)(implicit logger: Logger,
    cache: JMap[AnyRef, AnyRef], classLoaderCache: ClassLoaderCache): Seq[File] = {
    val classifierName = classifier.name.getOrElse("<none>")
    val classified = dependencies.collect{ case m: BoundMavenDependency =>
      m.copy(mavenDependency = m.mavenDependency.copy(classifier = classifier))
    }
    classified.flatMap{ dependency =>
      try {
        dependency.exportedJars
      } catch {
        case NonFatal(_) =>
          logger.resolver(
            s"ensime: could not find $classifierName of ${dependency.mavenDependency.serialize}")
          Seq.empty
      }
    }
  }

  /** Find and convert all (transitive) dependencies of a build to a sequence of ENSIME projects. */
  def findProjects(root: BaseBuild)(implicit logger: Logger, cache: JMap[AnyRef, AnyRef], classLoaderCache: ClassLoaderCache): Seq[EnsimeProject] = {
    def asProject(base: BaseBuild) = EnsimeProject(
      id = EnsimeProjectId(
        project = base.name,
        config = "compile"
      ),
      depends = base.transitiveDependencies.collect{
        case b: BaseBuild => EnsimeProjectId(b.name, "compile")
      },
      sources = base.sources.filter(_.isDirectory),
      targets = Seq(base.compileTarget),
      scalacOptions = base.scalacOptions.toList,
      javacOptions = Nil, // TODO get javac options from base build
      libraryJars = base.transitiveDependencies.flatMap(_.exportedClasspathArray).toList,
      librarySources = resolveWithClassifier(base.transitiveDependencies, Classifier.sources),
      libraryDocs = resolveWithClassifier(base.transitiveDependencies, Classifier.javadoc)
    )

    Seq(asProject(root)) ++ root.transitiveDependencies.collect{ case b: BaseBuild =>
      asProject(b)
    }
  }

  final case class EnsimeProjectId(
    project: String,
    config: String
  )

  final case class EnsimeProject(
    id: EnsimeProjectId,
    depends: Seq[EnsimeProjectId],
    sources: Seq[File],
    targets: Seq[File],
    scalacOptions: Seq[String],
    javacOptions: Seq[String],
    libraryJars: Seq[File],
    librarySources: Seq[File],
    libraryDocs: Seq[File]
  )

  /* EnsimeModules are required for backwards-compatibility with clients.
   * They can automatically be derived from projects (see method EnsimeModule.fromProjects). */
  private final case class EnsimeModule(
    name: String,
    mainRoots: Set[File],
    testRoots: Set[File],
    targets: Set[File],
    testTargets: Set[File],
    dependsOnNames: Set[String],
    compileJars: Set[File],
    runtimeJars: Set[File],
    testJars: Set[File],
    sourceJars: Set[File],
    docJars: Set[File]
  )
  private object EnsimeModule {
    def fromProjects(p: Iterable[EnsimeProject]): EnsimeModule = {
      val name = p.head.id.project
      val deps = for {
        s <- p
        d <- s.depends
        if d.project != name
      } yield d.project
      val (mains, tests) = p.toSet.partition(_.id.config == "compile")
      val mainSources = mains.flatMap(_.sources)
      val mainTargets = mains.flatMap(_.targets)
      val mainJars = mains.flatMap(_.libraryJars)
      val testSources = tests.flatMap(_.sources)
      val testTargets = tests.flatMap(_.targets)
      val testJars = tests.flatMap(_.libraryJars).toSet -- mainJars
      val sourceJars = p.flatMap(_.librarySources).toSet
      val docJars = p.flatMap(_.libraryDocs).toSet
      EnsimeModule(
        name, mainSources, testSources, mainTargets, testTargets, deps.toSet,
        mainJars, Set.empty, testJars, sourceJars, docJars
      )
    }
  }

  final case class EnsimeConfig(
    scalaCompilerJars: Seq[File],
    ensimeServerJars: Seq[File],
    ensimeServerVersion: String,
    rootDir: File,
    cacheDir: File,
    javaHome: File,
    name: String,
    scalaVersion: String,
    javaSources: Seq[File],
    javaFlags: Seq[String],
    projects: Seq[EnsimeProject]
  ) {
    def toSexp: String = EnsimeConfig.sexp(this)
  }
  object EnsimeConfig {
    private def sexp(value: Any): String = value match {
      case s: String => s""""$s""""
      case f: File => sexp(f.getAbsolutePath)
      case (k, v) => s":$k ${sexp(v)}\n"
      case xss: Traversable[_] if xss.isEmpty => "nil"
      case xss: Traversable[_] => xss.map(sexp(_)).mkString("(", " ", ")")
      case EnsimeProjectId(project, config) => sexp(List(
        "project" -> project,
        "config" -> config
      ))
      case proj: EnsimeProject => sexp(List(
        "id" -> proj.id,
        "depends" -> proj.depends,
        "sources" -> proj.sources,
        "targets" -> proj.targets,
        "scalac-options" -> proj.scalacOptions,
        "javac-options" -> proj.javacOptions,
        "library-jars" -> proj.libraryJars,
        "library-sources" -> proj.librarySources,
        "library-docs" -> proj.libraryDocs
      ))
      case conf: EnsimeConfig => sexp(List(
        "root-dir" -> conf.rootDir,
        "cache-dir" -> conf.cacheDir,
        "scala-compiler-jars" -> conf.scalaCompilerJars,
        "ensime-server-jars" -> conf.ensimeServerJars,
        "ensime-server-version" -> conf.ensimeServerVersion,
        "name" -> conf.name,
        "scala-version" -> conf.scalaVersion,
        "java-home" -> conf.javaHome,
        "java-flags" -> conf.javaFlags,
        "java-sources" -> conf.javaSources,
        "projects" -> conf.projects,
        // subprojects are required for backwards-compatibility with older clients
        // (ensime-server does not require them)
        "subprojects" -> conf.projects.groupBy(_.id.project).mapValues(EnsimeModule.fromProjects)//conf.projects.flatMap(proj => EnsimeModule.fromProjects(conf.projects))
      ))
      case module: EnsimeModule => sexp(List(
        "name" -> module.name,
        "source-roots" -> (module.mainRoots ++ module.testRoots),
        "targets" -> module.targets,
        "test-targets" -> module.testTargets,
        "depends-on-modules" -> module.dependsOnNames,
        "compile-deps" -> module.compileJars,
        "runtime-deps" -> module.runtimeJars,
        "test-deps" -> module.testJars,
        "doc-jars"-> module.docJars,
        "reference-source-roots" -> module.sourceJars
      ))
    }
  }

}
