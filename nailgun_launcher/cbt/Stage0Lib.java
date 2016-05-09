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
import static cbt.NailgunLauncher.*;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;

public class Stage0Lib{
  public static void _assert(Boolean condition, Object msg){
    if(!condition){
      throw new AssertionError("Assertion failed: "+msg);
    }
  }

  public static int runMain(String cls, String[] args, ClassLoader cl, SecurityManager defaultSecurityManager) throws Exception{
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
      System.setSecurityManager(defaultSecurityManager);
    }
  }

  public static Object get(Object object, String method) throws Exception{
    return object.getClass().getMethod( method ).invoke(object);
  }

  public static String classpath( String... files ){
    Arrays.sort(files);
    return join( pathSeparator, files );
  }

  public static Boolean compile(
    Boolean changed, Long start, String classpath, String target,
    EarlyDependencies earlyDeps, List<File> sourceFiles, SecurityManager defaultSecurityManager
  ) throws Exception{
    File statusFile = new File( new File(target) + ".last-success" );
    Long lastSuccessfullCompile = statusFile.lastModified();
    for( File file: sourceFiles ){
      if( file.lastModified() > lastSuccessfullCompile ){
        changed = true;
        break;
      }
    }
    if(changed){
      List<String> zincArgs = new ArrayList<String>(
        Arrays.asList(
          new String[]{ 
            "-scala-compiler", earlyDeps.scalaCompiler_2_11_8_File,
            "-scala-library", earlyDeps.scalaLibrary_2_11_8_File,
            "-scala-extra", earlyDeps.scalaReflect_2_11_8_File,
            "-sbt-interface", earlyDeps.sbtInterface_0_13_9_File,
            "-compiler-interface", earlyDeps.compilerInterface_0_13_9_File,
            "-cp", classpath,
            "-d", target
          }
        )
      );

      for( File f: sourceFiles ){
        zincArgs.add(f.toString());
      }

      PrintStream oldOut = System.out;
      try{
        System.setOut(System.err);
        int exitCode = runMain( "com.typesafe.zinc.Main", zincArgs.toArray(new String[zincArgs.size()]), earlyDeps.zinc, defaultSecurityManager );
        if( exitCode == 0 ){
          Files.write( statusFile.toPath(), "".getBytes());
          Files.setLastModifiedTime( statusFile.toPath(), FileTime.fromMillis(start) );
        } else {
          System.exit( exitCode );
        }
      } finally {
        System.setOut(oldOut);
      }
    }
    return changed;
  }

  public static ClassLoader classLoader( String file ) throws Exception{
    return new CbtURLClassLoader(
      new URL[]{ new URL("file:"+file) }
    );
  }
  public static ClassLoader classLoader( String file, ClassLoader parent ) throws Exception{
    return new CbtURLClassLoader(
      new URL[]{ new URL("file:"+file) }, parent
    );
  }
  public static void download(URL urlString, Path target, String sha1) throws Exception {
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

  public static String sha1(byte[] bytes) throws Exception {
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

  public static String[] append( String[] array, String item ){
    String[] copy = Arrays.copyOf(array, array.length + 1);
    copy[array.length] = item;
    return copy;
  }
}