// This file was auto-generated using `cbt tools cbtEarlyDependencies`
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

  String scalaReflect_2_11_8_File;
  String scalaCompiler_2_11_8_File;
  String scalaXml_1_0_5_File;
  String scalaLibrary_2_11_8_File;
  String zinc_0_3_9_File;
  String incrementalCompiler_0_13_9_File;
  String compilerInterface_0_13_9_File;
  String scalaCompiler_2_10_5_File;
  String sbtInterface_0_13_9_File;
  String scalaReflect_2_10_5_File;
  String scalaLibrary_2_10_5_File;

  public EarlyDependencies(
    String mavenCache, String mavenUrl, ClassLoaderCache2<ClassLoader> classLoaderCache, ClassLoader rootClassLoader
  ) throws Exception {
    scalaReflect_2_11_8_File = mavenCache + "/org/scala-lang/scala-reflect/2.11.8/scala-reflect-2.11.8.jar";
    scalaCompiler_2_11_8_File = mavenCache + "/org/scala-lang/scala-compiler/2.11.8/scala-compiler-2.11.8.jar";
    scalaXml_1_0_5_File = mavenCache + "/org/scala-lang/modules/scala-xml_2.11/1.0.5/scala-xml_2.11-1.0.5.jar";
    scalaLibrary_2_11_8_File = mavenCache + "/org/scala-lang/scala-library/2.11.8/scala-library-2.11.8.jar";
    zinc_0_3_9_File = mavenCache + "/com/typesafe/zinc/zinc/0.3.9/zinc-0.3.9.jar";
    incrementalCompiler_0_13_9_File = mavenCache + "/com/typesafe/sbt/incremental-compiler/0.13.9/incremental-compiler-0.13.9.jar";
    compilerInterface_0_13_9_File = mavenCache + "/com/typesafe/sbt/compiler-interface/0.13.9/compiler-interface-0.13.9-sources.jar";
    scalaCompiler_2_10_5_File = mavenCache + "/org/scala-lang/scala-compiler/2.10.5/scala-compiler-2.10.5.jar";
    sbtInterface_0_13_9_File = mavenCache + "/com/typesafe/sbt/sbt-interface/0.13.9/sbt-interface-0.13.9.jar";
    scalaReflect_2_10_5_File = mavenCache + "/org/scala-lang/scala-reflect/2.10.5/scala-reflect-2.10.5.jar";
    scalaLibrary_2_10_5_File = mavenCache + "/org/scala-lang/scala-library/2.10.5/scala-library-2.10.5.jar";

    download(new URL(mavenUrl + "/org/scala-lang/scala-reflect/2.11.8/scala-reflect-2.11.8.jar"), Paths.get(scalaReflect_2_11_8_File), "b74530deeba742ab4f3134de0c2da0edc49ca361");
    download(new URL(mavenUrl + "/org/scala-lang/scala-compiler/2.11.8/scala-compiler-2.11.8.jar"), Paths.get(scalaCompiler_2_11_8_File), "fe1285c9f7b58954c5ef6d80b59063569c065e9a");

    // org.scala-lang:scala-library:2.10.5
    download(new URL(mavenUrl + "/org/scala-lang/scala-library/2.10.5/scala-library-2.10.5.jar"), Paths.get(scalaLibrary_2_10_5_File), "57ac67a6cf6fd591e235c62f8893438e8d10431d");

    String[] scalaLibrary_2_10_5_ClasspathArray = new String[]{scalaLibrary_2_10_5_File};
    String scalaLibrary_2_10_5_Classpath = classpath( scalaLibrary_2_10_5_ClasspathArray );
    ClassLoader scalaLibrary_2_10_5_ =
      classLoaderCache.contains( scalaLibrary_2_10_5_Classpath )
      ? classLoaderCache.get( scalaLibrary_2_10_5_Classpath )
      : classLoaderCache.put( classLoader( scalaLibrary_2_10_5_File, rootClassLoader ), scalaLibrary_2_10_5_Classpath );

    // org.scala-lang:scala-reflect:2.10.5
    download(new URL(mavenUrl + "/org/scala-lang/scala-reflect/2.10.5/scala-reflect-2.10.5.jar"), Paths.get(scalaReflect_2_10_5_File), "7392facb48876c67a89fcb086112b195f5f6bbc3");

    String[] scalaReflect_2_10_5_ClasspathArray = new String[]{scalaLibrary_2_10_5_File, scalaReflect_2_10_5_File};
    String scalaReflect_2_10_5_Classpath = classpath( scalaReflect_2_10_5_ClasspathArray );
    ClassLoader scalaReflect_2_10_5_ =
      classLoaderCache.contains( scalaReflect_2_10_5_Classpath )
      ? classLoaderCache.get( scalaReflect_2_10_5_Classpath )
      : classLoaderCache.put( classLoader( scalaReflect_2_10_5_File, scalaLibrary_2_10_5_ ), scalaReflect_2_10_5_Classpath );

    // com.typesafe.sbt:sbt-interface:0.13.9
    download(new URL(mavenUrl + "/com/typesafe/sbt/sbt-interface/0.13.9/sbt-interface-0.13.9.jar"), Paths.get(sbtInterface_0_13_9_File), "29848631415402c81b732e919be88f268df37250");

    String[] sbtInterface_0_13_9_ClasspathArray = new String[]{sbtInterface_0_13_9_File, scalaLibrary_2_10_5_File, scalaReflect_2_10_5_File};
    String sbtInterface_0_13_9_Classpath = classpath( sbtInterface_0_13_9_ClasspathArray );
    ClassLoader sbtInterface_0_13_9_ =
      classLoaderCache.contains( sbtInterface_0_13_9_Classpath )
      ? classLoaderCache.get( sbtInterface_0_13_9_Classpath )
      : classLoaderCache.put( classLoader( sbtInterface_0_13_9_File, scalaReflect_2_10_5_ ), sbtInterface_0_13_9_Classpath );

    // org.scala-lang:scala-compiler:2.10.5
    download(new URL(mavenUrl + "/org/scala-lang/scala-compiler/2.10.5/scala-compiler-2.10.5.jar"), Paths.get(scalaCompiler_2_10_5_File), "f0f5bb444ca26a6e489af3dd35e24f7e2d2d118e");

    String[] scalaCompiler_2_10_5_ClasspathArray = new String[]{sbtInterface_0_13_9_File, scalaCompiler_2_10_5_File, scalaLibrary_2_10_5_File, scalaReflect_2_10_5_File};
    String scalaCompiler_2_10_5_Classpath = classpath( scalaCompiler_2_10_5_ClasspathArray );
    ClassLoader scalaCompiler_2_10_5_ =
      classLoaderCache.contains( scalaCompiler_2_10_5_Classpath )
      ? classLoaderCache.get( scalaCompiler_2_10_5_Classpath )
      : classLoaderCache.put( classLoader( scalaCompiler_2_10_5_File, sbtInterface_0_13_9_ ), scalaCompiler_2_10_5_Classpath );

    // com.typesafe.sbt:compiler-interface:0.13.9
    download(new URL(mavenUrl + "/com/typesafe/sbt/compiler-interface/0.13.9/compiler-interface-0.13.9-sources.jar"), Paths.get(compilerInterface_0_13_9_File), "2311addbed1182916ad00f83c57c0eeca1af382b");

    String[] compilerInterface_0_13_9_ClasspathArray = new String[]{compilerInterface_0_13_9_File, sbtInterface_0_13_9_File, scalaCompiler_2_10_5_File, scalaLibrary_2_10_5_File, scalaReflect_2_10_5_File};
    String compilerInterface_0_13_9_Classpath = classpath( compilerInterface_0_13_9_ClasspathArray );
    ClassLoader compilerInterface_0_13_9_ =
      classLoaderCache.contains( compilerInterface_0_13_9_Classpath )
      ? classLoaderCache.get( compilerInterface_0_13_9_Classpath )
      : classLoaderCache.put( classLoader( compilerInterface_0_13_9_File, scalaCompiler_2_10_5_ ), compilerInterface_0_13_9_Classpath );

    // com.typesafe.sbt:incremental-compiler:0.13.9
    download(new URL(mavenUrl + "/com/typesafe/sbt/incremental-compiler/0.13.9/incremental-compiler-0.13.9.jar"), Paths.get(incrementalCompiler_0_13_9_File), "fbbf1cadbed058aa226643e83543c35de43b13f0");

    String[] incrementalCompiler_0_13_9_ClasspathArray = new String[]{compilerInterface_0_13_9_File, incrementalCompiler_0_13_9_File, sbtInterface_0_13_9_File, scalaCompiler_2_10_5_File, scalaLibrary_2_10_5_File, scalaReflect_2_10_5_File};
    String incrementalCompiler_0_13_9_Classpath = classpath( incrementalCompiler_0_13_9_ClasspathArray );
    ClassLoader incrementalCompiler_0_13_9_ =
      classLoaderCache.contains( incrementalCompiler_0_13_9_Classpath )
      ? classLoaderCache.get( incrementalCompiler_0_13_9_Classpath )
      : classLoaderCache.put( classLoader( incrementalCompiler_0_13_9_File, compilerInterface_0_13_9_ ), incrementalCompiler_0_13_9_Classpath );

    // com.typesafe.zinc:zinc:0.3.9
    download(new URL(mavenUrl + "/com/typesafe/zinc/zinc/0.3.9/zinc-0.3.9.jar"), Paths.get(zinc_0_3_9_File), "46a4556d1f36739879f4b2cc19a73d12b3036e9a");

    String[] zinc_0_3_9_ClasspathArray = new String[]{compilerInterface_0_13_9_File, incrementalCompiler_0_13_9_File, sbtInterface_0_13_9_File, zinc_0_3_9_File, scalaCompiler_2_10_5_File, scalaLibrary_2_10_5_File, scalaReflect_2_10_5_File};
    String zinc_0_3_9_Classpath = classpath( zinc_0_3_9_ClasspathArray );
    ClassLoader zinc_0_3_9_ =
      classLoaderCache.contains( zinc_0_3_9_Classpath )
      ? classLoaderCache.get( zinc_0_3_9_Classpath )
      : classLoaderCache.put( classLoader( zinc_0_3_9_File, incrementalCompiler_0_13_9_ ), zinc_0_3_9_Classpath );

    // org.scala-lang:scala-library:2.11.8
    download(new URL(mavenUrl + "/org/scala-lang/scala-library/2.11.8/scala-library-2.11.8.jar"), Paths.get(scalaLibrary_2_11_8_File), "ddd5a8bced249bedd86fb4578a39b9fb71480573");

    String[] scalaLibrary_2_11_8_ClasspathArray = new String[]{scalaLibrary_2_11_8_File};
    String scalaLibrary_2_11_8_Classpath = classpath( scalaLibrary_2_11_8_ClasspathArray );
    ClassLoader scalaLibrary_2_11_8_ =
      classLoaderCache.contains( scalaLibrary_2_11_8_Classpath )
      ? classLoaderCache.get( scalaLibrary_2_11_8_Classpath )
      : classLoaderCache.put( classLoader( scalaLibrary_2_11_8_File, rootClassLoader ), scalaLibrary_2_11_8_Classpath );

    // org.scala-lang.modules:scala-xml_2.11:1.0.5
    download(new URL(mavenUrl + "/org/scala-lang/modules/scala-xml_2.11/1.0.5/scala-xml_2.11-1.0.5.jar"), Paths.get(scalaXml_1_0_5_File), "77ac9be4033768cf03cc04fbd1fc5e5711de2459");

    String[] scalaXml_1_0_5_ClasspathArray = new String[]{scalaXml_1_0_5_File, scalaLibrary_2_11_8_File};
    String scalaXml_1_0_5_Classpath = classpath( scalaXml_1_0_5_ClasspathArray );
    ClassLoader scalaXml_1_0_5_ =
      classLoaderCache.contains( scalaXml_1_0_5_Classpath )
      ? classLoaderCache.get( scalaXml_1_0_5_Classpath )
      : classLoaderCache.put( classLoader( scalaXml_1_0_5_File, scalaLibrary_2_11_8_ ), scalaXml_1_0_5_Classpath );
  
    classLoader = scalaXml_1_0_5_;
    classpathArray = scalaXml_1_0_5_ClasspathArray;

    zinc = zinc_0_3_9_;
  }
}
