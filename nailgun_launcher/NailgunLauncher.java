package cbt;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.*;
import java.nio.file.*;
import static java.io.File.pathSeparator;
import java.security.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

/**
 * This launcher allows to start the JVM without loading anything else permanently into its
 * classpath except for the launcher itself. That's why it is written in Java without
 * dependencies outside the JDK.
 */
public class NailgunLauncher{
  public static String SCALA_VERSION = "2.11.8";
  public static String SCALA_XML_VERSION = "1.0.5";
  public static String ZINC_VERSION = "0.3.9";

  public static String CBT_HOME = System.getenv("CBT_HOME");
  public static String NAILGUN = System.getenv("NAILGUN");
  public static String TARGET = System.getenv("TARGET");
  public static String STAGE1 = CBT_HOME + "/stage1/";
  public static String MAVEN_CACHE = CBT_HOME + "/cache/maven";
  public static String MAVEN_URL = "https://repo1.maven.org/maven2";

  /**
   * Persistent cache for caching classloaders for the JVM life time. Can be used as needed by user
   * code to improve startup time.
   */
  public static ConcurrentHashMap<String, Object> classLoaderCacheKeys = new ConcurrentHashMap<String,Object>();
  public static ConcurrentHashMap<Object, ClassLoader> classLoaderCacheValues = new ConcurrentHashMap<Object,ClassLoader>();

  public static SecurityManager defaultSecurityManager = System.getSecurityManager();

  public static long lastSuccessfullCompile = 0;
  static ClassLoader stage1classLoader = null;
  public static ClassLoader stage2classLoader = null;

  public static void main(String[] args) throws ClassNotFoundException,
                                                NoSuchMethodException,
                                                IllegalAccessException,
                                                InvocationTargetException,
                                                MalformedURLException,
                                                IOException,
                                                NoSuchAlgorithmException {
    long now = System.currentTimeMillis();
    //System.err.println("ClassLoader: "+stage1classLoader);
    //System.err.println("lastSuccessfullCompile: "+lastSuccessfullCompile);
    //System.err.println("now: "+now);

    _assert(CBT_HOME != null, CBT_HOME);
    _assert(NAILGUN != null, NAILGUN);
    _assert(TARGET != null, TARGET);
    _assert(STAGE1 != null, STAGE1);

    if(args[0].equals("check-alive")){
      System.exit(33);
      return;
    }

    List<File> stage1SourceFiles = new ArrayList<File>();
    for( File f: new File(STAGE1).listFiles() ){
      if( f.isFile() && f.toString().endsWith(".scala") ){
        stage1SourceFiles.add(f);
      }
    }

    Boolean changed = lastSuccessfullCompile == 0;
    for( File file: stage1SourceFiles ){
      if( file.lastModified() > lastSuccessfullCompile ){
        changed = true;
        //System.err.println("File change: "+file.lastModified());
        break;
      }
    }

    if(changed){
      EarlyDependencies earlyDeps = new EarlyDependencies();
      int exitCode = zinc(earlyDeps, stage1SourceFiles);
      if( exitCode == 0 ){
        lastSuccessfullCompile = now;
      } else {
        System.exit( exitCode );
      }

      ClassLoader nailgunClassLoader;
      if( classLoaderCacheKeys.containsKey( NAILGUN+TARGET ) ){
        nailgunClassLoader = cacheGet( NAILGUN+TARGET );
      } else {
        nailgunClassLoader = cachePut( classLoader(NAILGUN+TARGET, earlyDeps.stage1), NAILGUN+TARGET ); // FIXME: key is wrong here, should be full CP
      }

      stage1classLoader = classLoader(STAGE1+TARGET, nailgunClassLoader);
      stage2classLoader = null;
    }

    try{
      Integer exitCode =
        (Integer) stage1classLoader
          .loadClass("cbt.Stage1")
          .getMethod("run", String[].class, ClassLoader.class, Boolean.class)
          .invoke( null, (Object) args, stage1classLoader, changed);
      System.exit(exitCode);
    }catch(Exception e){
      System.err.println(stage1classLoader);
      throw e;
    }
  }

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

  static int zinc( EarlyDependencies earlyDeps, List<File> sourceFiles ) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException{
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

  static ClassLoader classLoader( String file ) throws MalformedURLException{
    return new CbtURLClassLoader(
      new URL[]{ new URL("file:"+file) }
    );
  }
  static ClassLoader classLoader( String file, ClassLoader parent ) throws MalformedURLException{
    return new CbtURLClassLoader(
      new URL[]{ new URL("file:"+file) }, parent
    );
  }
  static ClassLoader cacheGet( String key ){
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
