package cbt
import scala.collection.immutable.Seq
import java.net._
import java.io.{Console=>_,_}
import java.nio.file._
class ToolsTasks(
  lib: Lib,
  args: Seq[String],
  cwd: File,
  classLoaderCache: ClassLoaderCache,
  cache: File,
  cbtHome: File,
  cbtHasChanged: Boolean
){
  private val paths = CbtPaths(cbtHome, cache)
  import paths._
  private def Resolver( urls: URL* ) = MavenResolver(cbtHasChanged,mavenCache,urls: _*)
  implicit val logger: Logger = lib.logger
  def createMain: Unit = lib.createMain( cwd )
  def createBasicBuild: Unit = lib.createBasicBuild( cwd )
  def createBuildBuild: Unit = lib.createBuildBuild( cwd )
  def resolve = {
    ClassPath.flatten(
      args(1).split(",").toVector.map{
        d =>
          val v = d.split(":")
          Resolver(mavenCentral).bindOne(MavenDependency(v(0),v(1),v(2))).classpath
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
    val d = Resolver(mavenCentral).bindOne(
      MavenDependency(
        "com.lihaoyi","ammonite-repl_2.11.8",args.lift(1).getOrElse("0.5.8")
      )
    )
    // FIXME: this does not work quite yet, throws NoSuchFileException: /ammonite/repl/frontend/ReplBridge$.class
    lib.runMain(
      "ammonite.repl.Main", Seq(), d.classLoader(classLoaderCache)
    )
  }
  def scala = {
    val version = args.lift(1).getOrElse(constants.scalaVersion)
    val scalac = new ScalaCompilerDependency( cbtHasChanged, mavenCache, version )
    lib.runMain(
      "scala.tools.nsc.MainGenericRunner", Seq("-cp", scalac.classpath.string), scalac.classLoader(classLoaderCache)
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
      Resolver(mavenCentral).bindOne(MavenDependency("org.scala-lang.modules","scala-xml_"+scalaMajorVersion,scalaXmlVersion)),
      Resolver(mavenCentral).bindOne(MavenDependency("org.scala-lang","scala-library",scalaVersion))
    )

    val zinc = Resolver(mavenCentral).bindOne(MavenDependency("com.typesafe.zinc","zinc",zincVersion))

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
    download(new URL(mavenUrl + "${d.basePath}.jar"), Paths.get(${n}File), "${d.jarSha1}");

    String[] ${n}ClasspathArray = new String[]{${deps.sortBy(_.jar).map(valName(_)+"File").mkString(", ")}};
    String ${n}Classpath = classpath( ${n}ClasspathArray );
    ClassLoader $n =
      classLoaderCache.contains( ${n}Classpath )
      ? classLoaderCache.get( ${n}Classpath )
      : classLoaderCache.put( classLoader( ${n}File, $parentString ), ${n}Classpath );"""
      }
    }
    val assignments = codeEach(zinc) ++ codeEach(scalaXml)
    val files = scalaDeps ++ transitive(scalaXml) ++ transitive(zinc)
    //{ case (name, dep) => s"$name =\n      ${tree(dep, 4)};" }.mkString("\n\n    ")
    val code = s"""// This file was auto-generated using `cbt tools cbtEarlyDependencies`
package cbt;
import java.io.*;
import java.nio.file.*;
import java.net.*;
import java.security.*;
import static cbt.Stage0Lib.*;
import static cbt.NailgunLauncher.*;

class EarlyDependencies{

  /** ClassLoader for stage1 */
  ClassLoader classLoader;
  String[] classpathArray;
  /** ClassLoader for zinc */
  ClassLoader zinc;

${files.map(d => s"""  String ${valName(d)}File;""").mkString("\n")}

  public EarlyDependencies(
    String mavenCache, String mavenUrl, ClassLoaderCache2<ClassLoader> classLoaderCache, ClassLoader rootClassLoader
  ) throws Exception {
${files.map(d => s"""    ${valName(d)}File = mavenCache + "${d.basePath}.jar";""").mkString("\n")}

${scalaDeps.map(d => s"""    download(new URL(mavenUrl + "${d.basePath}.jar"), Paths.get(${valName(d)}File), "${d.jarSha1}");""").mkString("\n")}
${assignments.mkString("\n")}
  
    classLoader = scalaXml_${scalaXmlVersion.replace(".","_")}_;
    classpathArray = scalaXml_${scalaXmlVersion.replace(".","_")}_ClasspathArray;

    zinc = zinc_${zincVersion.replace(".","_")}_;
  }
}
"""
    val file = nailgun ++ ("/" ++ "EarlyDependencies.java")
    Files.write( file.toPath, code.getBytes )
    println( Console.GREEN ++ "Wrote " ++ file.string ++ Console.RESET )
  }
}
