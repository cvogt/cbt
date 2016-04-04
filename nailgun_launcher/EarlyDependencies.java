// This file was auto-generated using `cbt admin cbtEarlyDependencies`
package cbt;
import java.io.*;
import java.nio.file.*;
import java.net.*;
import java.security.*;
import static cbt.Stage0Lib.*;
import static cbt.NailgunLauncher.*;

class EarlyDependencies{

  /** ClassLoader for stage1 */
  ClassLoader stage1;
  /** ClassLoader for zinc */
  ClassLoader zinc;

  String scalaReflect_2_11_8_File = MAVEN_CACHE + "/org/scala-lang/scala-reflect/2.11.8/scala-reflect-2.11.8.jar";
  String scalaCompiler_2_11_8_File = MAVEN_CACHE + "/org/scala-lang/scala-compiler/2.11.8/scala-compiler-2.11.8.jar";
  String scalaXml_1_0_5_File = MAVEN_CACHE + "/org/scala-lang/modules/scala-xml_2.11/1.0.5/scala-xml_2.11-1.0.5.jar";
  String scalaLibrary_2_11_8_File = MAVEN_CACHE + "/org/scala-lang/scala-library/2.11.8/scala-library-2.11.8.jar";
  String zinc_0_3_9_File = MAVEN_CACHE + "/com/typesafe/zinc/zinc/0.3.9/zinc-0.3.9.jar";
  String incrementalCompiler_0_13_9_File = MAVEN_CACHE + "/com/typesafe/sbt/incremental-compiler/0.13.9/incremental-compiler-0.13.9.jar";
  String compilerInterface_0_13_9_File = MAVEN_CACHE + "/com/typesafe/sbt/compiler-interface/0.13.9/compiler-interface-0.13.9-sources.jar";
  String scalaCompiler_2_10_5_File = MAVEN_CACHE + "/org/scala-lang/scala-compiler/2.10.5/scala-compiler-2.10.5.jar";
  String sbtInterface_0_13_9_File = MAVEN_CACHE + "/com/typesafe/sbt/sbt-interface/0.13.9/sbt-interface-0.13.9.jar";
  String scalaReflect_2_10_5_File = MAVEN_CACHE + "/org/scala-lang/scala-reflect/2.10.5/scala-reflect-2.10.5.jar";
  String scalaLibrary_2_10_5_File = MAVEN_CACHE + "/org/scala-lang/scala-library/2.10.5/scala-library-2.10.5.jar";

  public EarlyDependencies() throws MalformedURLException, IOException, NoSuchAlgorithmException{
    download(new URL(MAVEN_URL + "/org/scala-lang/scala-reflect/2.11.8/scala-reflect-2.11.8.jar"), Paths.get(scalaReflect_2_11_8_File), "b74530deeba742ab4f3134de0c2da0edc49ca361");
    download(new URL(MAVEN_URL + "/org/scala-lang/scala-compiler/2.11.8/scala-compiler-2.11.8.jar"), Paths.get(scalaCompiler_2_11_8_File), "fe1285c9f7b58954c5ef6d80b59063569c065e9a");

    // org.scala-lang:scala-library:2.10.5
    download(new URL(MAVEN_URL + "/org/scala-lang/scala-library/2.10.5/scala-library-2.10.5.jar"), Paths.get(scalaLibrary_2_10_5_File), "57ac67a6cf6fd591e235c62f8893438e8d10431d");
    ClassLoader scalaLibrary_2_10_5_ = cachePut(
      classLoader( scalaLibrary_2_10_5_File ),
      scalaLibrary_2_10_5_File
    );

    // org.scala-lang:scala-reflect:2.10.5
    download(new URL(MAVEN_URL + "/org/scala-lang/scala-reflect/2.10.5/scala-reflect-2.10.5.jar"), Paths.get(scalaReflect_2_10_5_File), "7392facb48876c67a89fcb086112b195f5f6bbc3");
    ClassLoader scalaReflect_2_10_5_ = cachePut(
      classLoader( scalaReflect_2_10_5_File, scalaLibrary_2_10_5_ ),
      scalaLibrary_2_10_5_File, scalaReflect_2_10_5_File
    );

    // com.typesafe.sbt:sbt-interface:0.13.9
    download(new URL(MAVEN_URL + "/com/typesafe/sbt/sbt-interface/0.13.9/sbt-interface-0.13.9.jar"), Paths.get(sbtInterface_0_13_9_File), "29848631415402c81b732e919be88f268df37250");
    ClassLoader sbtInterface_0_13_9_ = cachePut(
      classLoader( sbtInterface_0_13_9_File, scalaReflect_2_10_5_ ),
      sbtInterface_0_13_9_File, scalaLibrary_2_10_5_File, scalaReflect_2_10_5_File
    );

    // org.scala-lang:scala-compiler:2.10.5
    download(new URL(MAVEN_URL + "/org/scala-lang/scala-compiler/2.10.5/scala-compiler-2.10.5.jar"), Paths.get(scalaCompiler_2_10_5_File), "f0f5bb444ca26a6e489af3dd35e24f7e2d2d118e");
    ClassLoader scalaCompiler_2_10_5_ = cachePut(
      classLoader( scalaCompiler_2_10_5_File, sbtInterface_0_13_9_ ),
      sbtInterface_0_13_9_File, scalaCompiler_2_10_5_File, scalaLibrary_2_10_5_File, scalaReflect_2_10_5_File
    );

    // com.typesafe.sbt:compiler-interface:0.13.9
    download(new URL(MAVEN_URL + "/com/typesafe/sbt/compiler-interface/0.13.9/compiler-interface-0.13.9-sources.jar"), Paths.get(compilerInterface_0_13_9_File), "2311addbed1182916ad00f83c57c0eeca1af382b");
    ClassLoader compilerInterface_0_13_9_ = cachePut(
      classLoader( compilerInterface_0_13_9_File, scalaCompiler_2_10_5_ ),
      compilerInterface_0_13_9_File, sbtInterface_0_13_9_File, scalaCompiler_2_10_5_File, scalaLibrary_2_10_5_File, scalaReflect_2_10_5_File
    );

    // com.typesafe.sbt:incremental-compiler:0.13.9
    download(new URL(MAVEN_URL + "/com/typesafe/sbt/incremental-compiler/0.13.9/incremental-compiler-0.13.9.jar"), Paths.get(incrementalCompiler_0_13_9_File), "fbbf1cadbed058aa226643e83543c35de43b13f0");
    ClassLoader incrementalCompiler_0_13_9_ = cachePut(
      classLoader( incrementalCompiler_0_13_9_File, compilerInterface_0_13_9_ ),
      compilerInterface_0_13_9_File, incrementalCompiler_0_13_9_File, sbtInterface_0_13_9_File, scalaCompiler_2_10_5_File, scalaLibrary_2_10_5_File, scalaReflect_2_10_5_File
    );

    // com.typesafe.zinc:zinc:0.3.9
    download(new URL(MAVEN_URL + "/com/typesafe/zinc/zinc/0.3.9/zinc-0.3.9.jar"), Paths.get(zinc_0_3_9_File), "46a4556d1f36739879f4b2cc19a73d12b3036e9a");
    ClassLoader zinc_0_3_9_ = cachePut(
      classLoader( zinc_0_3_9_File, incrementalCompiler_0_13_9_ ),
      compilerInterface_0_13_9_File, incrementalCompiler_0_13_9_File, sbtInterface_0_13_9_File, zinc_0_3_9_File, scalaCompiler_2_10_5_File, scalaLibrary_2_10_5_File, scalaReflect_2_10_5_File
    );

    // org.scala-lang:scala-library:2.11.8
    download(new URL(MAVEN_URL + "/org/scala-lang/scala-library/2.11.8/scala-library-2.11.8.jar"), Paths.get(scalaLibrary_2_11_8_File), "ddd5a8bced249bedd86fb4578a39b9fb71480573");
    ClassLoader scalaLibrary_2_11_8_ = cachePut(
      classLoader( scalaLibrary_2_11_8_File ),
      scalaLibrary_2_11_8_File
    );

    // org.scala-lang.modules:scala-xml_2.11:1.0.5
    download(new URL(MAVEN_URL + "/org/scala-lang/modules/scala-xml_2.11/1.0.5/scala-xml_2.11-1.0.5.jar"), Paths.get(scalaXml_1_0_5_File), "77ac9be4033768cf03cc04fbd1fc5e5711de2459");
    ClassLoader scalaXml_1_0_5_ = cachePut(
      classLoader( scalaXml_1_0_5_File, scalaLibrary_2_11_8_ ),
      scalaXml_1_0_5_File, scalaLibrary_2_11_8_File
    );
  
    stage1 = scalaXml_1_0_5_;

    zinc = zinc_0_3_9_;
  }
}
