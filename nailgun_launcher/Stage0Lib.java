package cbt;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import static java.io.File.pathSeparator;
import static cbt.Stage0Lib.*;
import static cbt.NailgunLauncher.*;

public class Stage0Lib{
  public static void _assert(Boolean condition, Object msg){
    if(!condition){
      throw new AssertionError("Assertion failed: "+msg);
    }
  }

  public static int runMain(String cls, String[] args, ClassLoader cl) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException{
    try{
      System.setSecurityManager( new TrapSecurityManager() );
      cl.loadClass(cls)
        .getMethod("main", String[].class)
        .invoke( null, (Object) args);
      return 0;
    }catch( InvocationTargetException exception ){
      Throwable cause = exception.getCause();
      if(cause instanceof TrappedExitCode){
        return ((TrappedExitCode) cause).exitCode;
      }
      throw exception;
    } finally {
      System.setSecurityManager(NailgunLauncher.defaultSecurityManager);
    }
  }

  public static int zinc( EarlyDependencies earlyDeps, List<File> sourceFiles ) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException{
    String cp = NAILGUN+TARGET + pathSeparator + earlyDeps.scalaXml_1_0_5_File + pathSeparator + earlyDeps.scalaLibrary_2_11_8_File;
    List<String> zincArgs = new ArrayList<String>(
      Arrays.asList(
        new String[]{ 
          "-scala-compiler", earlyDeps.scalaCompiler_2_11_8_File,
          "-scala-library", earlyDeps.scalaLibrary_2_11_8_File,
          "-scala-extra", earlyDeps.scalaReflect_2_11_8_File,
          "-sbt-interface", earlyDeps.sbtInterface_0_13_9_File,
          "-compiler-interface", earlyDeps.compilerInterface_0_13_9_File,
          "-cp", cp,
          "-d", STAGE1+TARGET
        }
      )
    );

    for( File f: sourceFiles ){
      zincArgs.add(f.toString());
    }

    PrintStream oldOut = System.out;
    try{
      System.setOut(System.err);
      return runMain( "com.typesafe.zinc.Main", zincArgs.toArray(new String[zincArgs.size()]), earlyDeps.zinc );
    } finally {
      System.setOut(oldOut);
    }
  }

  public static ClassLoader classLoader( String file ) throws MalformedURLException{
    return new CbtURLClassLoader(
      new URL[]{ new URL("file:"+file) }
    );
  }
  public static ClassLoader classLoader( String file, ClassLoader parent ) throws MalformedURLException{
    return new CbtURLClassLoader(
      new URL[]{ new URL("file:"+file) }, parent
    );
  }
  public static ClassLoader cacheGet( String key ){
    return classLoaderCacheValues.get(
      classLoaderCacheKeys.get( key )
    );
  }
  public static ClassLoader cachePut( ClassLoader classLoader, String... jars ){
    String key = join( pathSeparator, jars );
    Object keyObject = new Object();
    classLoaderCacheKeys.put( key, keyObject );
    classLoaderCacheValues.put( keyObject, classLoader );
    return classLoader;
  }

  public static void download(URL urlString, Path target, String sha1) throws IOException, NoSuchAlgorithmException {
    final Path unverified = Paths.get(target+".unverified");
    if(!Files.exists(target)) {
      new File(target.toString()).getParentFile().mkdirs();
      System.err.println("downloading " + urlString);
      System.err.println("to " + target);
      final InputStream stream = urlString.openStream();
      Files.copy(stream, unverified, StandardCopyOption.REPLACE_EXISTING);
      stream.close();
      final String checksum = sha1(Files.readAllBytes(unverified));
      if(sha1 == null || sha1.toUpperCase().equals(checksum)) {
        Files.move(unverified, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } else {
        System.err.println(target + " checksum does not match.\nExpected: |" + sha1 + "|\nFound:    |" + checksum + "|");
        System.exit(1);
      }
    }
  }

  public static String sha1(byte[] bytes) throws NoSuchAlgorithmException {
    final MessageDigest sha1 = MessageDigest.getInstance("SHA1");
    sha1.update(bytes, 0, bytes.length);
    return (new HexBinaryAdapter()).marshal(sha1.digest());
  }

  public static String join(String separator, String[] parts){
    String result = parts[0];
    for(int i = 1; i < parts.length; i++){
      result += separator + parts[i];
    }
    return result;
  }
}