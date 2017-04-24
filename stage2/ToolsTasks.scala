package cbt
import java.net._
import java.io.{Console=>_,_}
import java.nio.file._
class ToolsTasks(
  lib: Lib,
  args: Seq[String],
  cwd: File,
  cache: File,
  cbtHome: File,
  cbtLastModified: Long
)(implicit classLoaderCache: ClassLoaderCache){
  def apply: String = "Available methods: " ++ lib.taskNames(getClass).mkString("  ")
  override def toString = lib.usage(this.getClass, super.toString)
  private val paths = CbtPaths(cbtHome, cache)
  import paths._
  implicit val logger: Logger = lib.logger
  implicit val transientCache: java.util.Map[AnyRef,AnyRef] = new java.util.HashMap
  private def Resolver( urls: URL* ) = MavenResolver(cbtLastModified,mavenCache,urls: _*)
  val scaffold = new Scaffold(logger)
  def createMain: Unit = scaffold.createMain( cwd )
  def createBuild: Unit = scaffold.createBuild( cwd )
  def `search-class` = java.awt.Desktop.getDesktop().browse(new URI(
    "http://search.maven.org/#search%7Cga%7C1%7Cc%3A%22" ++ URLEncoder.encode(args(1),"UTF-8") ++ "%22"
  ))
  def gui = NailgunLauncher.main(Array(
    "0.0",
    (cbtHome / "tools" / "gui").getAbsolutePath,
    "0",
    "run",
    cwd.getAbsolutePath,
    constants.scalaMajorVersion
  ))
  def resolve = {
    ClassPath.flatten(
      args(1).split(",").toVector.map{
        d =>
          val v = d.split(":")
          Resolver(mavenCentral, sonatypeSnapshots).bindOne(MavenDependency(v(0),v(1),v(2))).classpath
      }
    )
  }
  def dependencyTree = {
    args(1).split(",").toVector.map{
      d =>
        val v = d.split(":")
        Resolver(mavenCentral).bindOne(MavenDependency(v(0),v(1),v(2))).dependencyTree
    }.mkString("\n\n")
  }
  def amm = ammonite
  def ammonite = {
    val version = args.lift(1).getOrElse(constants.scalaVersion)
    val ammonite = Resolver(mavenCentral).bindOne(
      MavenDependency(
        "com.lihaoyi","ammonite-repl_"++constants.scalaVersion,args.lift(1).getOrElse("0.5.8")
      )
    )
    // FIXME: this does not work quite yet, throws NoSuchFileException: /ammonite/repl/frontend/ReplBridge$.class
    ammonite.runMain(
      "ammonite.repl.Main", args.drop(2)
    )
  }
  def scala = {
    val version = args.lift(1).getOrElse(constants.scalaVersion)
    val scalac = new ScalaCompilerDependency( cbtLastModified, mavenCache, version )
    val _args = Seq("-cp", scalac.classpath.string) ++ args.drop(2)
    scalac.runMain(
      "scala.tools.nsc.MainGenericRunner", _args
    )
  }
  def cbtEarlyDependencies = {
    val scalaVersion = args.lift(1).getOrElse(constants.scalaVersion)
    val scalaMajorVersion = scalaVersion.split("\\.").take(2).mkString(".")
    val scalaXmlVersion = args.lift(2).getOrElse(constants.scalaXmlVersion)
    val zincVersion = args.lift(3).getOrElse(constants.zincVersion)
    val scalaDeps = Seq(
      Resolver(mavenCentral).bindOne(MavenDependency("org.scala-lang","scala-reflect",scalaVersion)),
      Resolver(mavenCentral).bindOne(MavenDependency("org.scala-lang","scala-compiler",scalaVersion))
    )

    val scalaXml = Dependencies(
      Resolver(mavenCentral).bind(
        MavenDependency("org.scala-lang.modules","scala-xml_"+scalaMajorVersion,scalaXmlVersion),
        MavenDependency("org.scala-lang","scala-library",scalaVersion)
      )
    )

    val zinc = Resolver(mavenCentral).bindOne(MavenDependency("com.typesafe.zinc","zinc",zincVersion))
    val sbtVersion =
      zinc.dependencies
        .collect{ case d @
          BoundMavenDependency(
            _, _, MavenDependency( "com.typesafe.sbt", "compiler-interface", _, Classifier.sources, _), _, _
          ) => d
        }
        .headOption
        .getOrElse( throw new Exception(s"cannot find compiler-interface in zinc $zincVersion dependencies: "++zinc.dependencies.toString) )
        .mavenDependency
        .version

    def valName(dep: BoundMavenDependency) = {
      val words = dep.artifactId.split("_").head.split("-")
      words(0) ++ words.drop(1).map(s => s(0).toString.toUpperCase ++ s.drop(1)).mkString ++ "_" ++ dep.version.replace(".","_") ++ "_"
    }

    def jarVal(dep: BoundMavenDependency) = "_" + valName(dep) +"Jar"
    def transitive(dep: Dependency) = (dep +: lib.transitiveDependencies(dep).reverse).collect{case d: BoundMavenDependency => d}
    def codeEach(dep: Dependency) = {
      transitive(dep).tails.map(_.reverse).toVector.reverse.drop(1).map{
        deps =>
          val d = deps.last
          val parents = deps.dropRight(1)
          val parentString = if(parents.isEmpty) "rootClassLoader"  else ( valName(parents.last) )
          val n = valName(d)
          s"""
    // ${d.groupId}:${d.artifactId}:${d.version}
    String[] ${n}ClasspathArray = new String[]{${deps.sortBy(_.jar).map(valName(_)+"File").mkString(", ")}};
    ClassLoader $n = loadDependency(
      mavenUrl + "${d.basePath(true)}.jar",
      ${n}File,
      "${d.jarSha1}",
      classLoaderCache,
      $parentString,
      ${n}ClasspathArray
    );"""
      }
    }
    val assignments = codeEach(zinc) ++ codeEach(scalaXml)
    val files = scalaDeps ++ transitive(scalaXml) ++ transitive(zinc)
    val _scalaVersion = scalaVersion.replace(".","_")
    val _scalaXmlVersion = scalaXmlVersion.replace(".","_")
    val _zincVersion = zincVersion.replace(".","_")
    val _sbtVersion = sbtVersion.replace(".","_")
    //{ case (name, dep) => s"$name =\n      ${tree(dep, 4)};" }.mkString("\n\n    ")
    val code = s"""// This file was auto-generated using `cbt tools cbtEarlyDependencies`
package cbt;
import java.io.*;
import java.nio.file.*;
import java.net.*;
import java.security.*;
import java.util.*;
import static cbt.Stage0Lib.*;
import static cbt.NailgunLauncher.*;

public class EarlyDependencies{

  public static String scalaVersion = "$scalaVersion";
  public static String scalaXmlVersion = "$scalaXmlVersion";
  public static String zincVersion = "$zincVersion";

  /** ClassLoader for stage1 */
  ClassLoader classLoader;
  String[] classpathArray;
  /** ClassLoader for zinc */
  ClassLoader zinc;

  String scalaCompiler_File;
  String scalaLibrary_File;
  String scalaReflect_File;
  String sbtInterface_File;
  String compilerInterface_File;

  public EarlyDependencies(
    String mavenCache, String mavenUrl, ClassLoaderCache classLoaderCache, ClassLoader rootClassLoader
  ) throws Throwable {
${files.map(d => s"""    String ${valName(d)}File = mavenCache + "${d.basePath(true)}.jar";""").mkString("\n")}

${scalaDeps.map(d => s"""    download(new URL(mavenUrl + "${d.basePath(true)}.jar"), Paths.get(${valName(d)}File), "${d.jarSha1}");""").mkString("\n")}
${assignments.mkString("\n")}

    classLoader = scalaXml_${_scalaXmlVersion}_;
    classpathArray = scalaXml_${_scalaXmlVersion}_ClasspathArray;

    zinc = zinc_${_zincVersion}_;

    scalaCompiler_File = scalaCompiler_${_scalaVersion}_File;
    scalaLibrary_File = scalaLibrary_${_scalaVersion}_File;
    scalaReflect_File = scalaReflect_${_scalaVersion}_File;
    sbtInterface_File = sbtInterface_${_sbtVersion}_File;
    compilerInterface_File = compilerInterface_${_sbtVersion}_File;
  }
}
"""
    val file = nailgun ++ ("/" ++ "EarlyDependencies.java")
    lib.writeIfChanged( file, code )
    println( Console.GREEN ++ "Wrote " ++ file.string ++ Console.RESET )
  }
}
