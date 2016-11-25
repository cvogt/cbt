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
  String zinc_0_3_12_File;
  String incrementalCompiler_0_13_12_File;
  String compilerInterface_0_13_12_File;
  String scalaCompiler_2_10_6_File;
  String sbtInterface_0_13_12_File;
  String scalaReflect_2_10_6_File;
  String scalaLibrary_2_10_6_File;

  public EarlyDependencies(
    String mavenCache, String mavenUrl, JavaCache<ClassLoader> classLoaderCache, ClassLoader rootClassLoader
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
    download(new URL(mavenUrl + "/org/scala-lang/scala-library/2.10.6/scala-library-2.10.6.jar"), Paths.get(scalaLibrary_2_10_6_File), "421989aa8f95a05a4f894630aad96b8c7b828732");

    String[] scalaLibrary_2_10_6_ClasspathArray = new String[]{scalaLibrary_2_10_6_File};
    String scalaLibrary_2_10_6_Classpath = classpath( scalaLibrary_2_10_6_ClasspathArray );
    ClassLoader scalaLibrary_2_10_6_ =
      classLoaderCache.contains( scalaLibrary_2_10_6_Classpath )
      ? classLoaderCache.get( scalaLibrary_2_10_6_Classpath )
      : classLoaderCache.put( classLoader( scalaLibrary_2_10_6_File, rootClassLoader ), scalaLibrary_2_10_6_Classpath );

    // org.scala-lang:scala-reflect:2.10.6
    download(new URL(mavenUrl + "/org/scala-lang/scala-reflect/2.10.6/scala-reflect-2.10.6.jar"), Paths.get(scalaReflect_2_10_6_File), "3259f3df0f166f017ef5b2d385445808398c316c");

    String[] scalaReflect_2_10_6_ClasspathArray = new String[]{scalaLibrary_2_10_6_File, scalaReflect_2_10_6_File};
    String scalaReflect_2_10_6_Classpath = classpath( scalaReflect_2_10_6_ClasspathArray );
    ClassLoader scalaReflect_2_10_6_ =
      classLoaderCache.contains( scalaReflect_2_10_6_Classpath )
      ? classLoaderCache.get( scalaReflect_2_10_6_Classpath )
      : classLoaderCache.put( classLoader( scalaReflect_2_10_6_File, scalaLibrary_2_10_6_ ), scalaReflect_2_10_6_Classpath );

    // com.typesafe.sbt:sbt-interface:0.13.12
    download(new URL(mavenUrl + "/com/typesafe/sbt/sbt-interface/0.13.12/sbt-interface-0.13.12.jar"), Paths.get(sbtInterface_0_13_12_File), "fcc7875c02f0d4641fac0518121bd71475d3909b");

    String[] sbtInterface_0_13_12_ClasspathArray = new String[]{sbtInterface_0_13_12_File, scalaLibrary_2_10_6_File, scalaReflect_2_10_6_File};
    String sbtInterface_0_13_12_Classpath = classpath( sbtInterface_0_13_12_ClasspathArray );
    ClassLoader sbtInterface_0_13_12_ =
      classLoaderCache.contains( sbtInterface_0_13_12_Classpath )
      ? classLoaderCache.get( sbtInterface_0_13_12_Classpath )
      : classLoaderCache.put( classLoader( sbtInterface_0_13_12_File, scalaReflect_2_10_6_ ), sbtInterface_0_13_12_Classpath );

    // org.scala-lang:scala-compiler:2.10.6
    download(new URL(mavenUrl + "/org/scala-lang/scala-compiler/2.10.6/scala-compiler-2.10.6.jar"), Paths.get(scalaCompiler_2_10_6_File), "9b15174852f5b6bb1edbf303d5722286a0a54011");

    String[] scalaCompiler_2_10_6_ClasspathArray = new String[]{sbtInterface_0_13_12_File, scalaCompiler_2_10_6_File, scalaLibrary_2_10_6_File, scalaReflect_2_10_6_File};
    String scalaCompiler_2_10_6_Classpath = classpath( scalaCompiler_2_10_6_ClasspathArray );
    ClassLoader scalaCompiler_2_10_6_ =
      classLoaderCache.contains( scalaCompiler_2_10_6_Classpath )
      ? classLoaderCache.get( scalaCompiler_2_10_6_Classpath )
      : classLoaderCache.put( classLoader( scalaCompiler_2_10_6_File, sbtInterface_0_13_12_ ), scalaCompiler_2_10_6_Classpath );

    // com.typesafe.sbt:compiler-interface:0.13.12
    download(new URL(mavenUrl + "/com/typesafe/sbt/compiler-interface/0.13.12/compiler-interface-0.13.12-sources.jar"), Paths.get(compilerInterface_0_13_12_File), "d9c3270576e162bf017b146af262364c2db87a32");

    String[] compilerInterface_0_13_12_ClasspathArray = new String[]{compilerInterface_0_13_12_File, sbtInterface_0_13_12_File, scalaCompiler_2_10_6_File, scalaLibrary_2_10_6_File, scalaReflect_2_10_6_File};
    String compilerInterface_0_13_12_Classpath = classpath( compilerInterface_0_13_12_ClasspathArray );
    ClassLoader compilerInterface_0_13_12_ =
      classLoaderCache.contains( compilerInterface_0_13_12_Classpath )
      ? classLoaderCache.get( compilerInterface_0_13_12_Classpath )
      : classLoaderCache.put( classLoader( compilerInterface_0_13_12_File, scalaCompiler_2_10_6_ ), compilerInterface_0_13_12_Classpath );

    // com.typesafe.sbt:incremental-compiler:0.13.12
    download(new URL(mavenUrl + "/com/typesafe/sbt/incremental-compiler/0.13.12/incremental-compiler-0.13.12.jar"), Paths.get(incrementalCompiler_0_13_12_File), "259f6d24a5a3791bb233787d6a8e639c4ab86fe5");

    String[] incrementalCompiler_0_13_12_ClasspathArray = new String[]{compilerInterface_0_13_12_File, incrementalCompiler_0_13_12_File, sbtInterface_0_13_12_File, scalaCompiler_2_10_6_File, scalaLibrary_2_10_6_File, scalaReflect_2_10_6_File};
    String incrementalCompiler_0_13_12_Classpath = classpath( incrementalCompiler_0_13_12_ClasspathArray );
    ClassLoader incrementalCompiler_0_13_12_ =
      classLoaderCache.contains( incrementalCompiler_0_13_12_Classpath )
      ? classLoaderCache.get( incrementalCompiler_0_13_12_Classpath )
      : classLoaderCache.put( classLoader( incrementalCompiler_0_13_12_File, compilerInterface_0_13_12_ ), incrementalCompiler_0_13_12_Classpath );

    // com.typesafe.zinc:zinc:0.3.12
    download(new URL(mavenUrl + "/com/typesafe/zinc/zinc/0.3.12/zinc-0.3.12.jar"), Paths.get(zinc_0_3_12_File), "c4339e93f5b7273f49ad026248f4fdb1d4d6c7c4");

    String[] zinc_0_3_12_ClasspathArray = new String[]{compilerInterface_0_13_12_File, incrementalCompiler_0_13_12_File, sbtInterface_0_13_12_File, zinc_0_3_12_File, scalaCompiler_2_10_6_File, scalaLibrary_2_10_6_File, scalaReflect_2_10_6_File};
    String zinc_0_3_12_Classpath = classpath( zinc_0_3_12_ClasspathArray );
    ClassLoader zinc_0_3_12_ =
      classLoaderCache.contains( zinc_0_3_12_Classpath )
      ? classLoaderCache.get( zinc_0_3_12_Classpath )
      : classLoaderCache.put( classLoader( zinc_0_3_12_File, incrementalCompiler_0_13_12_ ), zinc_0_3_12_Classpath );

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

    zinc = zinc_0_3_12_;
  }
}
