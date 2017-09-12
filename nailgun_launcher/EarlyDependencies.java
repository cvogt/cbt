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

  public static String scalaVersion = "2.11.11";
  public static String scalaXmlVersion = "1.0.6";
  public static String zincVersion = "0.3.15";

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
    String scalaReflect_2_11_11_File = mavenCache + "/org/scala-lang/scala-reflect/2.11.11/scala-reflect-2.11.11.jar";
    String scalaCompiler_2_11_11_File = mavenCache + "/org/scala-lang/scala-compiler/2.11.11/scala-compiler-2.11.11.jar";
    String scalaXml_1_0_6_File = mavenCache + "/org/scala-lang/modules/scala-xml_2.11/1.0.6/scala-xml_2.11-1.0.6.jar";
    String scalaLibrary_2_11_11_File = mavenCache + "/org/scala-lang/scala-library/2.11.11/scala-library-2.11.11.jar";
    String zinc_0_3_15_File = mavenCache + "/com/typesafe/zinc/zinc/0.3.15/zinc-0.3.15.jar";
    String incrementalCompiler_0_13_15_File = mavenCache + "/com/typesafe/sbt/incremental-compiler/0.13.15/incremental-compiler-0.13.15.jar";
    String compilerInterface_0_13_15_File = mavenCache + "/com/typesafe/sbt/compiler-interface/0.13.15/compiler-interface-0.13.15-sources.jar";
    String scalaCompiler_2_10_6_File = mavenCache + "/org/scala-lang/scala-compiler/2.10.6/scala-compiler-2.10.6.jar";
    String sbtInterface_0_13_15_File = mavenCache + "/com/typesafe/sbt/sbt-interface/0.13.15/sbt-interface-0.13.15.jar";
    String scalaReflect_2_10_6_File = mavenCache + "/org/scala-lang/scala-reflect/2.10.6/scala-reflect-2.10.6.jar";
    String scalaLibrary_2_10_6_File = mavenCache + "/org/scala-lang/scala-library/2.10.6/scala-library-2.10.6.jar";

    download(new URL(mavenUrl + "/org/scala-lang/scala-reflect/2.11.11/scala-reflect-2.11.11.jar"), Paths.get(scalaReflect_2_11_11_File), "2addc7e09cf2e77e2243a5772bd0430c32c2b410");
    download(new URL(mavenUrl + "/org/scala-lang/scala-compiler/2.11.11/scala-compiler-2.11.11.jar"), Paths.get(scalaCompiler_2_11_11_File), "2f1568549280da6d0a332846cb7c27edae76fd10");

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

    // com.typesafe.sbt:sbt-interface:0.13.15
    String[] sbtInterface_0_13_15_ClasspathArray = new String[]{sbtInterface_0_13_15_File, scalaLibrary_2_10_6_File, scalaReflect_2_10_6_File};
    ClassLoader sbtInterface_0_13_15_ = loadDependency(
      mavenUrl + "/com/typesafe/sbt/sbt-interface/0.13.15/sbt-interface-0.13.15.jar",
      sbtInterface_0_13_15_File,
      "93fe450d5f5efb111397a34bc1fba0d50368a265",
      classLoaderCache,
      scalaReflect_2_10_6_,
      sbtInterface_0_13_15_ClasspathArray
    );

    // org.scala-lang:scala-compiler:2.10.6
    String[] scalaCompiler_2_10_6_ClasspathArray = new String[]{sbtInterface_0_13_15_File, scalaCompiler_2_10_6_File, scalaLibrary_2_10_6_File, scalaReflect_2_10_6_File};
    ClassLoader scalaCompiler_2_10_6_ = loadDependency(
      mavenUrl + "/org/scala-lang/scala-compiler/2.10.6/scala-compiler-2.10.6.jar",
      scalaCompiler_2_10_6_File,
      "9b15174852f5b6bb1edbf303d5722286a0a54011",
      classLoaderCache,
      sbtInterface_0_13_15_,
      scalaCompiler_2_10_6_ClasspathArray
    );

    // com.typesafe.sbt:compiler-interface:0.13.15
    String[] compilerInterface_0_13_15_ClasspathArray = new String[]{compilerInterface_0_13_15_File, sbtInterface_0_13_15_File, scalaCompiler_2_10_6_File, scalaLibrary_2_10_6_File, scalaReflect_2_10_6_File};
    ClassLoader compilerInterface_0_13_15_ = loadDependency(
      mavenUrl + "/com/typesafe/sbt/compiler-interface/0.13.15/compiler-interface-0.13.15-sources.jar",
      compilerInterface_0_13_15_File,
      "bad996ed4fc3e83b872525e9cd7b80d81b98a324",
      classLoaderCache,
      scalaCompiler_2_10_6_,
      compilerInterface_0_13_15_ClasspathArray
    );

    // com.typesafe.sbt:incremental-compiler:0.13.15
    String[] incrementalCompiler_0_13_15_ClasspathArray = new String[]{compilerInterface_0_13_15_File, incrementalCompiler_0_13_15_File, sbtInterface_0_13_15_File, scalaCompiler_2_10_6_File, scalaLibrary_2_10_6_File, scalaReflect_2_10_6_File};
    ClassLoader incrementalCompiler_0_13_15_ = loadDependency(
      mavenUrl + "/com/typesafe/sbt/incremental-compiler/0.13.15/incremental-compiler-0.13.15.jar",
      incrementalCompiler_0_13_15_File,
      "95e20d00b25a7aae19838009c11578b7e6b258ad",
      classLoaderCache,
      compilerInterface_0_13_15_,
      incrementalCompiler_0_13_15_ClasspathArray
    );

    // com.typesafe.zinc:zinc:0.3.15
    String[] zinc_0_3_15_ClasspathArray = new String[]{compilerInterface_0_13_15_File, incrementalCompiler_0_13_15_File, sbtInterface_0_13_15_File, zinc_0_3_15_File, scalaCompiler_2_10_6_File, scalaLibrary_2_10_6_File, scalaReflect_2_10_6_File};
    ClassLoader zinc_0_3_15_ = loadDependency(
      mavenUrl + "/com/typesafe/zinc/zinc/0.3.15/zinc-0.3.15.jar",
      zinc_0_3_15_File,
      "12e1f782684f2702e847faa0994eed4711270687",
      classLoaderCache,
      incrementalCompiler_0_13_15_,
      zinc_0_3_15_ClasspathArray
    );

    // org.scala-lang:scala-library:2.11.11
    String[] scalaLibrary_2_11_11_ClasspathArray = new String[]{scalaLibrary_2_11_11_File};
    ClassLoader scalaLibrary_2_11_11_ = loadDependency(
      mavenUrl + "/org/scala-lang/scala-library/2.11.11/scala-library-2.11.11.jar",
      scalaLibrary_2_11_11_File,
      "e283d2b7fde6504f6a86458b1f6af465353907cc",
      classLoaderCache,
      rootClassLoader,
      scalaLibrary_2_11_11_ClasspathArray
    );

    // org.scala-lang.modules:scala-xml_2.11:1.0.6
    String[] scalaXml_1_0_6_ClasspathArray = new String[]{scalaXml_1_0_6_File, scalaLibrary_2_11_11_File};
    ClassLoader scalaXml_1_0_6_ = loadDependency(
      mavenUrl + "/org/scala-lang/modules/scala-xml_2.11/1.0.6/scala-xml_2.11-1.0.6.jar",
      scalaXml_1_0_6_File,
      "4ebd108453e6455351c0ec50d32509ae1154fdb1",
      classLoaderCache,
      scalaLibrary_2_11_11_,
      scalaXml_1_0_6_ClasspathArray
    );

    classLoader = scalaXml_1_0_6_;
    classpathArray = scalaXml_1_0_6_ClasspathArray;

    zinc = zinc_0_3_15_;

    scalaCompiler_File = scalaCompiler_2_11_11_File;
    scalaLibrary_File = scalaLibrary_2_11_11_File;
    scalaReflect_File = scalaReflect_2_11_11_File;
    sbtInterface_File = sbtInterface_0_13_15_File;
    compilerInterface_File = compilerInterface_0_13_15_File;
  }
}
