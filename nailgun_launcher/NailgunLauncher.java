package cbt;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static cbt.Stage0Lib.*;

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
}
