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
  String zinc_0_3_12_File;
  String incrementalCompiler_0_13_12_File;
  String compilerInterface_0_13_12_File;
  String scalaCompiler_2_10_6_File;
  String sbtInterface_0_13_12_File;
  String scalaReflect_2_10_6_File;
  String scalaLibrary_2_10_6_File;

  public EarlyDependencies(
    String mavenCache, String mavenUrl, ClassLoaderCache classLoaderCache, ClassLoader rootClassLoader
  ) throws Throwable {
    scalaReflect_2_11_8_File = mavenCache + "/org/scala-lang/scala-reflect/2.11.8/scala-reflect-2.11.8.jar";
    scalaCompiler_2_11_8_File = mavenCache + "/org/scala-lang/scala-compiler/2.11.8/scala-compiler-2.11.8.jar";
    scalaXml_1_0_5_File = mavenCache + "/org/scala-lang/modules/scala-xml_2.11/1.0.5/scala-xml_2.11-1.0.5.jar";
    scalaLibrary_2_11_8_File = mavenCache + "/org/scala-lang/scala-library/2.11.8/scala-library-2.11.8.jar";
    zinc_0_3_12_File = mavenCache + "/com/typesafe/zinc/zinc/0.3.12/zinc-0.3.12.jar";
    incrementalCompiler_0_13_12_File = mavenCache + "/com/typesafe/sbt/incremental-compiler/0.13.12/incremental-compiler-0.13.12.jar";
    compilerInterface_0_13_12_File = mavenCache + "/com/typesafe/sbt/compiler-interface/0.13.12/compiler-interface-0.13.12-sources.jar";
    scalaCompiler_2_10_6_File = mavenCache + "/org/scala-lang/scala-compiler/2.10.6/scala-compiler-2.10.6.jar";
    sbtInterface_0_13_12_File = mavenCache + "/com/typesafe/sbt/sbt-interface/0.13.12/sbt-interface-0.13.12.jar";
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

    // com.typesafe.sbt:sbt-interface:0.13.12
    String[] sbtInterface_0_13_12_ClasspathArray = new String[]{sbtInterface_0_13_12_File, scalaLibrary_2_10_6_File, scalaReflect_2_10_6_File};
    ClassLoader sbtInterface_0_13_12_ = loadDependency(
      mavenUrl + "/com/typesafe/sbt/sbt-interface/0.13.12/sbt-interface-0.13.12.jar",
      sbtInterface_0_13_12_File,
      "fcc7875c02f0d4641fac0518121bd71475d3909b",
      classLoaderCache,
      scalaReflect_2_10_6_,
      sbtInterface_0_13_12_ClasspathArray
    );

    // org.scala-lang:scala-compiler:2.10.6
    String[] scalaCompiler_2_10_6_ClasspathArray = new String[]{sbtInterface_0_13_12_File, scalaCompiler_2_10_6_File, scalaLibrary_2_10_6_File, scalaReflect_2_10_6_File};
    ClassLoader scalaCompiler_2_10_6_ = loadDependency(
      mavenUrl + "/org/scala-lang/scala-compiler/2.10.6/scala-compiler-2.10.6.jar",
      scalaCompiler_2_10_6_File,
      "9b15174852f5b6bb1edbf303d5722286a0a54011",
      classLoaderCache,
      sbtInterface_0_13_12_,
      scalaCompiler_2_10_6_ClasspathArray
    );

    // com.typesafe.sbt:compiler-interface:0.13.12
    String[] compilerInterface_0_13_12_ClasspathArray = new String[]{compilerInterface_0_13_12_File, sbtInterface_0_13_12_File, scalaCompiler_2_10_6_File, scalaLibrary_2_10_6_File, scalaReflect_2_10_6_File};
    ClassLoader compilerInterface_0_13_12_ = loadDependency(
      mavenUrl + "/com/typesafe/sbt/compiler-interface/0.13.12/compiler-interface-0.13.12-sources.jar",
      compilerInterface_0_13_12_File,
      "d9c3270576e162bf017b146af262364c2db87a32",
      classLoaderCache,
      scalaCompiler_2_10_6_,
      compilerInterface_0_13_12_ClasspathArray
    );

    // com.typesafe.sbt:incremental-compiler:0.13.12
    String[] incrementalCompiler_0_13_12_ClasspathArray = new String[]{compilerInterface_0_13_12_File, incrementalCompiler_0_13_12_File, sbtInterface_0_13_12_File, scalaCompiler_2_10_6_File, scalaLibrary_2_10_6_File, scalaReflect_2_10_6_File};
    ClassLoader incrementalCompiler_0_13_12_ = loadDependency(
      mavenUrl + "/com/typesafe/sbt/incremental-compiler/0.13.12/incremental-compiler-0.13.12.jar",
      incrementalCompiler_0_13_12_File,
      "259f6d24a5a3791bb233787d6a8e639c4ab86fe5",
      classLoaderCache,
      compilerInterface_0_13_12_,
      incrementalCompiler_0_13_12_ClasspathArray
    );

    // com.typesafe.zinc:zinc:0.3.12
    String[] zinc_0_3_12_ClasspathArray = new String[]{compilerInterface_0_13_12_File, incrementalCompiler_0_13_12_File, sbtInterface_0_13_12_File, zinc_0_3_12_File, scalaCompiler_2_10_6_File, scalaLibrary_2_10_6_File, scalaReflect_2_10_6_File};
    ClassLoader zinc_0_3_12_ = loadDependency(
      mavenUrl + "/com/typesafe/zinc/zinc/0.3.12/zinc-0.3.12.jar",
      zinc_0_3_12_File,
      "c4339e93f5b7273f49ad026248f4fdb1d4d6c7c4",
      classLoaderCache,
      incrementalCompiler_0_13_12_,
      zinc_0_3_12_ClasspathArray
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

    zinc = zinc_0_3_12_;
  }
}
