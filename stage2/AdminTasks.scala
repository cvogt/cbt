package cbt
import scala.collection.immutable.Seq
import java.io.{Console=>_,_}
import java.nio.file._
class AdminTasks(lib: Lib, args: Seq[String], cwd: File){
  implicit val logger: Logger = lib.logger
  def resolve = {
    ClassPath.flatten(
      args(1).split(",").toVector.map{
        d =>
          val v = d.split(":")
          new JavaDependency(v(0),v(1),v(2)).classpath
      }
    )
  }
  def dependencyTree = {
    args(1).split(",").toVector.map{
      d =>
        val v = d.split(":")
        new JavaDependency(v(0),v(1),v(2)).dependencyTree
    }.mkString("\n\n")
  }
  def amm = ammonite
  def ammonite = {
    val version = args.lift(1).getOrElse(constants.scalaVersion)
    val scalac = new ScalaCompilerDependency( version )
    val d = JavaDependency(
      "com.lihaoyi","ammonite-repl_2.11.7",args.lift(1).getOrElse("0.5.7")
    )
    // FIXME: this does not work quite yet, throws NoSuchFileException: /ammonite/repl/frontend/ReplBridge$.class
    lib.runMain(
      "ammonite.repl.Main", Seq(), d.classLoader(new ClassLoaderCache(logger))
    )
  }
  def scala = {
    val version = args.lift(1).getOrElse(constants.scalaVersion)
    val scalac = new ScalaCompilerDependency( version )
    lib.runMain(
      "scala.tools.nsc.MainGenericRunner", Seq("-cp", scalac.classpath.string), scalac.classLoader(new ClassLoaderCache(logger))
    )
  }
  def scaffoldBasicBuild: Unit = lib.scaffoldBasicBuild( cwd )
  def cbtEarlyDependencies = {
    val scalaVersion = args.lift(1).getOrElse(constants.scalaVersion)
    val scalaMajorVersion = scalaVersion.split("\\.").take(2).mkString(".")
    val scalaXmlVersion = args.lift(2).getOrElse(constants.scalaXmlVersion)
    val zincVersion = args.lift(3).getOrElse(constants.zincVersion)
    /*
    def tree(d: JavaDependency, indent: Int): String ={
      val dependencies = {
        if( d.dependencies.nonEmpty ){
          d.dependencies.map{
            case d: JavaDependency => tree(d,indent + 1)
          }.mkString(",\n" ++ ( "  " * indent ),",\n" ++ ( "  " * indent ), "")
        } else ""
      }
      (
        s"""new EarlyDependency( "${d.groupId}", "${d.artifactId}", "${d.version}", "${d.jarSha1}"$dependencies)"""
      )
    }*/
    val scalaDeps = Seq(
      JavaDependency("org.scala-lang","scala-reflect",scalaVersion),
      JavaDependency("org.scala-lang","scala-compiler",scalaVersion)
    )
    
    val scalaXml = Dependencies(
      JavaDependency("org.scala-lang.modules","scala-xml_"+scalaMajorVersion,scalaXmlVersion),
      JavaDependency("org.scala-lang","scala-library",scalaVersion)
    )

    val zinc = JavaDependency("com.typesafe.zinc","zinc",zincVersion)
    println(zinc.dependencyTree)

    def valName(dep: JavaDependency) = {
      val words = dep.artifactId.split("_").head.split("-")
      words(0) ++ words.drop(1).map(s => s(0).toString.toUpperCase ++ s.drop(1)).mkString ++ "_" ++ dep.version.replace(".","_") ++ "_"
    }

    def vals(d: JavaDependency) = s"""  """

    def jarVal(dep: JavaDependency) = "_" + valName(dep) +"Jar"
    def transitive(dep: Dependency) = (dep +: dep.transitiveDependencies.reverse).collect{case d: JavaDependency => d}
    def codeEach(dep: Dependency) = {    
      transitive(dep).tails.map(_.reverse).toVector.reverse.drop(1).map{
        deps =>
          val d = deps.last
          val parents = deps.dropRight(1)
          val parentString = if(parents.isEmpty) ""  else ( ", " ++ valName(parents.last) )
          val n = valName(d)
          s"""
    // ${d.groupId}:${d.artifactId}:${d.version}
    download(new URL(MAVEN_URL + "${d.basePath}.jar"), Paths.get(${n}File), "${d.jarSha1}");
    ClassLoader $n = cachePut(
      classLoader( ${n}File$parentString ),
      ${deps.sortBy(_.jar).map(valName(_)+"File").mkString(", ")}
    );"""
      }
    }
    val assignments = codeEach(zinc) ++ codeEach(scalaXml)
    //{ case (name, dep) => s"$name =\n      ${tree(dep, 4)};" }.mkString("\n\n    ")
    val code = s"""// This file was auto-generated using `cbt admin cbtEarlyDependencies`
package cbt;
import java.io.*;
import java.nio.file.*;
import java.net.*;
import java.security.*;
import static cbt.NailgunLauncher.*;

class EarlyDependencies{

  /** ClassLoader for stage1 */
  ClassLoader stage1;
  /** ClassLoader for zinc */
  ClassLoader zinc;

${(scalaDeps ++ transitive(scalaXml) ++ transitive(zinc)).map(d => s"""  String ${valName(d)}File = MAVEN_CACHE + "${d.basePath}.jar";""").mkString("\n")}

  public EarlyDependencies() throws MalformedURLException, IOException, NoSuchAlgorithmException{
${scalaDeps.map(d => s"""    download(new URL(MAVEN_URL + "${d.basePath}.jar"), Paths.get(${valName(d)}File), "${d.jarSha1}");""").mkString("\n")}
${assignments.mkString("\n")}
  
    stage1 = scalaXml_${scalaXmlVersion.replace(".","_")}_;

    zinc = zinc_${zincVersion.replace(".","_")}_;
  }
}
"""
    val file = paths.nailgun ++ ("/" ++ "EarlyDependencies.java")
    Files.write( file.toPath, code.getBytes )
    println( Console.GREEN ++ "Wrote " ++ file.string ++ Console.RESET )
  }
}
