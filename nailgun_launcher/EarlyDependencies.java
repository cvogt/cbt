// This file was auto-generated using `cbt tools cbtEarlyDependencies`
package cbt;
import java.io.*;
import java.nio.file.*;
import java.net.*;
import java.security.*;
import java.util.*;
import static cbt.Stage0Lib.*;
import static cbt.NailgunLauncher.*;

public class EarlyDependencies{

  public static String scalaVersion = "2.12.1";
  public static String scalaXmlVersion = "1.0.6";
  public static String zincVersion = "0.3.13";

  /** ClassLoader for stage1 */
  ClassLoader classLoader;
  String[] classpathArray;
  /** ClassLoader for zinc */
  ClassLoader zinc;

  String scalaReflect_2_12_1_File;
  String scalaCompiler_2_12_1_File;
  String scalaXml_1_0_6_File;
  String scalaLibrary_2_12_1_File;
  String zinc_0_3_13_File;
  String incrementalCompiler_0_13_13_File;
  String compilerInterface_0_13_13_File;
  String scalaCompiler_2_10_6_File;
  String sbtInterface_0_13_13_File;
  String scalaReflect_2_10_6_File;
  String scalaLibrary_2_10_6_File;

  public EarlyDependencies(
    String mavenCache, String mavenUrl, ClassLoaderCache classLoaderCache, ClassLoader rootClassLoader
  ) throws Throwable {
    scalaReflect_2_12_1_File = mavenCache + "/org/scala-lang/scala-reflect/2.12.1/scala-reflect-2.12.1.jar";
    scalaCompiler_2_12_1_File = mavenCache + "/org/scala-lang/scala-compiler/2.12.1/scala-compiler-2.12.1.jar";
    scalaXml_1_0_6_File = mavenCache + "/org/scala-lang/modules/scala-xml_2.12/1.0.6/scala-xml_2.12-1.0.6.jar";
    scalaLibrary_2_12_1_File = mavenCache + "/org/scala-lang/scala-library/2.12.1/scala-library-2.12.1.jar";
    zinc_0_3_13_File = mavenCache + "/com/typesafe/zinc/zinc/0.3.13/zinc-0.3.13.jar";
    incrementalCompiler_0_13_13_File = mavenCache + "/com/typesafe/sbt/incremental-compiler/0.13.13/incremental-compiler-0.13.13.jar";
    compilerInterface_0_13_13_File = mavenCache + "/com/typesafe/sbt/compiler-interface/0.13.13/compiler-interface-0.13.13-sources.jar";
    scalaCompiler_2_10_6_File = mavenCache + "/org/scala-lang/scala-compiler/2.10.6/scala-compiler-2.10.6.jar";
    sbtInterface_0_13_13_File = mavenCache + "/com/typesafe/sbt/sbt-interface/0.13.13/sbt-interface-0.13.13.jar";
    scalaReflect_2_10_6_File = mavenCache + "/org/scala-lang/scala-reflect/2.10.6/scala-reflect-2.10.6.jar";
    scalaLibrary_2_10_6_File = mavenCache + "/org/scala-lang/scala-library/2.10.6/scala-library-2.10.6.jar";

    download(new URL(mavenUrl + "/org/scala-lang/scala-reflect/2.12.1/scala-reflect-2.12.1.jar"), Paths.get(scalaReflect_2_12_1_File), "f6ae9e1c0204a3e92893d9a2188b276278f2074e");
    download(new URL(mavenUrl + "/org/scala-lang/scala-compiler/2.12.1/scala-compiler-2.12.1.jar"), Paths.get(scalaCompiler_2_12_1_File), "73ba7c52af8bcaae57eba191f74e2a9a946a1049");

    // org.scala-lang:scala-library:2.10.6
    String[] scalaLibrary_2_10_6_ClasspathArray = new String[]{scalaLibrary_2_10_6_File};
    ClassLoader scalaLibrary_2_10_6_ = loadDependency(
      mavenUrl + "/org/scala-lang/scala-library/2.10.6/scala-library-2.10.6.jar",
      scalaLibrary_2_10_6_File,
      "421989aa8f95a05a4f894630aad96b8c7b828732",
      classLoaderCache,
      rootClassLoader,
      scalaLibrary_2_10_6_ClasspathArray
    );

    // org.scala-lang:scala-reflect:2.10.6
    String[] scalaReflect_2_10_6_ClasspathArray = new String[]{scalaLibrary_2_10_6_File, scalaReflect_2_10_6_File};
    ClassLoader scalaReflect_2_10_6_ = loadDependency(
      mavenUrl + "/org/scala-lang/scala-reflect/2.10.6/scala-reflect-2.10.6.jar",
      scalaReflect_2_10_6_File,
      "3259f3df0f166f017ef5b2d385445808398c316c",
      classLoaderCache,
      scalaLibrary_2_10_6_,
      scalaReflect_2_10_6_ClasspathArray
    );

    // com.typesafe.sbt:sbt-interface:0.13.13
    String[] sbtInterface_0_13_13_ClasspathArray = new String[]{sbtInterface_0_13_13_File, scalaLibrary_2_10_6_File, scalaReflect_2_10_6_File};
    ClassLoader sbtInterface_0_13_13_ = loadDependency(
      mavenUrl + "/com/typesafe/sbt/sbt-interface/0.13.13/sbt-interface-0.13.13.jar",
      sbtInterface_0_13_13_File,
      "9367c5c7a835f9505774fc3b7c3a8146a1396f85",
      classLoaderCache,
      scalaReflect_2_10_6_,
      sbtInterface_0_13_13_ClasspathArray
    );

    // org.scala-lang:scala-compiler:2.10.6
    String[] scalaCompiler_2_10_6_ClasspathArray = new String[]{sbtInterface_0_13_13_File, scalaCompiler_2_10_6_File, scalaLibrary_2_10_6_File, scalaReflect_2_10_6_File};
    ClassLoader scalaCompiler_2_10_6_ = loadDependency(
      mavenUrl + "/org/scala-lang/scala-compiler/2.10.6/scala-compiler-2.10.6.jar",
      scalaCompiler_2_10_6_File,
      "9b15174852f5b6bb1edbf303d5722286a0a54011",
      classLoaderCache,
      sbtInterface_0_13_13_,
      scalaCompiler_2_10_6_ClasspathArray
    );

    // com.typesafe.sbt:compiler-interface:0.13.13
    String[] compilerInterface_0_13_13_ClasspathArray = new String[]{compilerInterface_0_13_13_File, sbtInterface_0_13_13_File, scalaCompiler_2_10_6_File, scalaLibrary_2_10_6_File, scalaReflect_2_10_6_File};
    ClassLoader compilerInterface_0_13_13_ = loadDependency(
      mavenUrl + "/com/typesafe/sbt/compiler-interface/0.13.13/compiler-interface-0.13.13-sources.jar",
      compilerInterface_0_13_13_File,
      "691bf88813bb34771c0ca4418d579bb652e1526f",
      classLoaderCache,
      scalaCompiler_2_10_6_,
      compilerInterface_0_13_13_ClasspathArray
    );

    // com.typesafe.sbt:incremental-compiler:0.13.13
    String[] incrementalCompiler_0_13_13_ClasspathArray = new String[]{compilerInterface_0_13_13_File, incrementalCompiler_0_13_13_File, sbtInterface_0_13_13_File, scalaCompiler_2_10_6_File, scalaLibrary_2_10_6_File, scalaReflect_2_10_6_File};
    ClassLoader incrementalCompiler_0_13_13_ = loadDependency(
      mavenUrl + "/com/typesafe/sbt/incremental-compiler/0.13.13/incremental-compiler-0.13.13.jar",
      incrementalCompiler_0_13_13_File,
      "65008fb90b965ea44d0959b8a2e214df206cda6e",
      classLoaderCache,
      compilerInterface_0_13_13_,
      incrementalCompiler_0_13_13_ClasspathArray
    );

    // com.typesafe.zinc:zinc:0.3.13
    String[] zinc_0_3_13_ClasspathArray = new String[]{compilerInterface_0_13_13_File, incrementalCompiler_0_13_13_File, sbtInterface_0_13_13_File, zinc_0_3_13_File, scalaCompiler_2_10_6_File, scalaLibrary_2_10_6_File, scalaReflect_2_10_6_File};
    ClassLoader zinc_0_3_13_ = loadDependency(
      mavenUrl + "/com/typesafe/zinc/zinc/0.3.13/zinc-0.3.13.jar",
      zinc_0_3_13_File,
      "ae0dcd6105d6e87ece2d181c5f0f7a79a966775a",
      classLoaderCache,
      incrementalCompiler_0_13_13_,
      zinc_0_3_13_ClasspathArray
    );

    // org.scala-lang:scala-library:2.12.1
    String[] scalaLibrary_2_12_1_ClasspathArray = new String[]{scalaLibrary_2_12_1_File};
    ClassLoader scalaLibrary_2_12_1_ = loadDependency(
      mavenUrl + "/org/scala-lang/scala-library/2.12.1/scala-library-2.12.1.jar",
      scalaLibrary_2_12_1_File,
      "dd235d04037dc6f4b6090257872dd35359a563ce",
      classLoaderCache,
      rootClassLoader,
      scalaLibrary_2_12_1_ClasspathArray
    );

    // org.scala-lang.modules:scala-xml_2.12:1.0.6
    String[] scalaXml_1_0_6_ClasspathArray = new String[]{scalaXml_1_0_6_File, scalaLibrary_2_12_1_File};
    ClassLoader scalaXml_1_0_6_ = loadDependency(
      mavenUrl + "/org/scala-lang/modules/scala-xml_2.12/1.0.6/scala-xml_2.12-1.0.6.jar",
      scalaXml_1_0_6_File,
      "e22de3366a698a9f744106fb6dda4335838cf6a7",
      classLoaderCache,
      scalaLibrary_2_12_1_,
      scalaXml_1_0_6_ClasspathArray
    );

    classLoader = scalaXml_1_0_6_;
    classpathArray = scalaXml_1_0_6_ClasspathArray;

    zinc = zinc_0_3_13_;
  }
}
