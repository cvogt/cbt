// This file was auto-generated using `cbt tools cbtEarlyDependencies`
package cbt;
import java.io.*;
import java.nio.file.*;
import java.net.*;
import java.security.*;
import java.util.*;
import static cbt.Stage0Lib.*;
import static cbt.NailgunLauncher.*;

class EarlyDependencies{

  /** ClassLoader for stage1 */
  ClassLoader classLoader;
  String[] classpathArray;
  /** ClassLoader for zinc */
  ClassLoader zinc;

  String scalaReflect_2_11_8_File;
  String scalaCompiler_2_11_8_File;
  String scalaXml_1_0_5_File;
  String scalaLibrary_2_11_8_File;
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
    scalaReflect_2_11_8_File = mavenCache + "/org/scala-lang/scala-reflect/2.11.8/scala-reflect-2.11.8.jar";
    scalaCompiler_2_11_8_File = mavenCache + "/org/scala-lang/scala-compiler/2.11.8/scala-compiler-2.11.8.jar";
    scalaXml_1_0_5_File = mavenCache + "/org/scala-lang/modules/scala-xml_2.11/1.0.5/scala-xml_2.11-1.0.5.jar";
    scalaLibrary_2_11_8_File = mavenCache + "/org/scala-lang/scala-library/2.11.8/scala-library-2.11.8.jar";
    zinc_0_3_13_File = mavenCache + "/com/typesafe/zinc/zinc/0.3.13/zinc-0.3.13.jar";
    incrementalCompiler_0_13_13_File = mavenCache + "/com/typesafe/sbt/incremental-compiler/0.13.13/incremental-compiler-0.13.13.jar";
    compilerInterface_0_13_13_File = mavenCache + "/com/typesafe/sbt/compiler-interface/0.13.13/compiler-interface-0.13.13-sources.jar";
    scalaCompiler_2_10_6_File = mavenCache + "/org/scala-lang/scala-compiler/2.10.6/scala-compiler-2.10.6.jar";
    sbtInterface_0_13_13_File = mavenCache + "/com/typesafe/sbt/sbt-interface/0.13.13/sbt-interface-0.13.13.jar";
    scalaReflect_2_10_6_File = mavenCache + "/org/scala-lang/scala-reflect/2.10.6/scala-reflect-2.10.6.jar";
    scalaLibrary_2_10_6_File = mavenCache + "/org/scala-lang/scala-library/2.10.6/scala-library-2.10.6.jar";

    download(new URL(mavenUrl + "/org/scala-lang/scala-reflect/2.11.8/scala-reflect-2.11.8.jar"), Paths.get(scalaReflect_2_11_8_File), "b74530deeba742ab4f3134de0c2da0edc49ca361");
    download(new URL(mavenUrl + "/org/scala-lang/scala-compiler/2.11.8/scala-compiler-2.11.8.jar"), Paths.get(scalaCompiler_2_11_8_File), "fe1285c9f7b58954c5ef6d80b59063569c065e9a");

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

    // org.scala-lang:scala-library:2.11.8
    String[] scalaLibrary_2_11_8_ClasspathArray = new String[]{scalaLibrary_2_11_8_File};
    ClassLoader scalaLibrary_2_11_8_ = loadDependency(
      mavenUrl + "/org/scala-lang/scala-library/2.11.8/scala-library-2.11.8.jar",
      scalaLibrary_2_11_8_File,
      "ddd5a8bced249bedd86fb4578a39b9fb71480573",
      classLoaderCache,
      rootClassLoader,
      scalaLibrary_2_11_8_ClasspathArray
    );

    // org.scala-lang.modules:scala-xml_2.11:1.0.5
    String[] scalaXml_1_0_5_ClasspathArray = new String[]{scalaXml_1_0_5_File, scalaLibrary_2_11_8_File};
    ClassLoader scalaXml_1_0_5_ = loadDependency(
      mavenUrl + "/org/scala-lang/modules/scala-xml_2.11/1.0.5/scala-xml_2.11-1.0.5.jar",
      scalaXml_1_0_5_File,
      "77ac9be4033768cf03cc04fbd1fc5e5711de2459",
      classLoaderCache,
      scalaLibrary_2_11_8_,
      scalaXml_1_0_5_ClasspathArray
    );

    classLoader = scalaXml_1_0_5_;
    classpathArray = scalaXml_1_0_5_ClasspathArray;

    zinc = zinc_0_3_13_;
  }
}
