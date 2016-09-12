package cbt;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static cbt.Stage0Lib.*;
import static java.io.File.pathSeparator;

/**
 * This launcher allows to start the JVM without loading anything else permanently into its
 * classpath except for the launcher itself. That's why it is written in Java without
 * dependencies outside the JDK.
 */
public class NailgunLauncher{
  /** Persistent cache for caching classloaders for the JVM life time. */
  private final static ClassLoaderCache2<ClassLoader> classLoaderCache = new ClassLoaderCache2<ClassLoader>(
    new ConcurrentHashMap<String,Object>(),
    new ConcurrentHashMap<Object,ClassLoader>()
  );

  public final static SecurityManager defaultSecurityManager = System.getSecurityManager();

  public static String TARGET = System.getenv("TARGET");
  private static String NAILGUN = "nailgun_launcher/";
  private static String STAGE1 = "stage1/";
  
  @SuppressWarnings("unchecked")
  public static Object getBuild( Object context ) throws Throwable{
    BuildStage1Result res = buildStage1(
      (Boolean) get(context, "cbtHasChangedCompat"),
      (Long) get(context, "startCompat"),
      ((File) get(context, "cache")).toString() + "/",
      ((File) get(context, "cbtHome")).toString(),
      ((File) get(context, "compatibilityTarget")).toString() + "/",
      new ClassLoaderCache2<ClassLoader>(
        (ConcurrentHashMap<String,Object>) get(context, "permanentKeys"),
        (ConcurrentHashMap<Object,ClassLoader>) get(context, "permanentClassLoaders")
      )
    );
    return
      res
        .classLoader
        .loadClass("cbt.Stage1")
        .getMethod( "getBuild", Object.class, Boolean.class )
        .invoke(null, context, res.changed);
  }

  public static void main( String[] args ) throws Throwable {
    Long _start = System.currentTimeMillis();
    if(args[0].equals("check-alive")){
      System.exit(33);
      return;
    }

    installProxySettings();
    String[] diff = args[0].split("\\.");
    long start = _start - (Long.parseLong(diff[0]) * 1000L) - Long.parseLong(diff[1]);
    
    _assert(System.getenv("CBT_HOME") != null, "environment variable CBT_HOME not defined");
    String CBT_HOME = System.getenv("CBT_HOME");
    String cache = CBT_HOME + "/cache/";
    String compatibilityTarget = CBT_HOME + "/compatibility/" + TARGET;
    BuildStage1Result res = buildStage1(
      false, start, cache, CBT_HOME, compatibilityTarget, classLoaderCache
    );

    try{
      System.exit(
        (Integer) res
          .classLoader
          .loadClass("cbt.Stage1")
          .getMethod(
            "run",
            String[].class, File.class, File.class, Boolean.class,
            File.class, Long.class, ConcurrentHashMap.class, ConcurrentHashMap.class
          )
          .invoke(
            null,
            (Object) args, new File(cache), new File(CBT_HOME), res.changed,
            new File(compatibilityTarget), start, classLoaderCache.keys, classLoaderCache.values
          )
      );
    } catch (Exception e) {
      Throwable t = unwrapInvocationTargetException(e);
      t.printStackTrace();
      System.exit(1);
    }
  }

  public static Throwable unwrapInvocationTargetException(Throwable e){
    if(e instanceof java.lang.reflect.InvocationTargetException){
      return unwrapInvocationTargetException(((java.lang.reflect.InvocationTargetException) e).getCause());
    } else{
      return e;
    }
  }

  public static BuildStage1Result buildStage1(
    Boolean changed, long start, String cache, String cbtHome, String compatibilityTarget, ClassLoaderCache2<ClassLoader> classLoaderCache
  ) throws Throwable {
    _assert(TARGET != null, "environment variable TARGET not defined");
    String nailgunTarget = cbtHome + "/" + NAILGUN + TARGET;
    String stage1Sources = cbtHome + "/" + STAGE1;
    String stage1Target = stage1Sources + TARGET;
    File compatibilitySources = new File(cbtHome + "/compatibility");
    String mavenCache = cache + "maven";
    String mavenUrl = "https://repo1.maven.org/maven2";

    ClassLoader rootClassLoader = new CbtURLClassLoader( new URL[]{}, ClassLoader.getSystemClassLoader().getParent() ); // wrap for caching
    EarlyDependencies earlyDeps = new EarlyDependencies(mavenCache, mavenUrl, classLoaderCache, rootClassLoader);

    ClassLoader compatibilityClassLoader;
    if(!compatibilityTarget.startsWith(cbtHome)){
      compatibilityClassLoader = classLoaderCache.get( compatibilityTarget );
    } else {
      List<File> compatibilitySourceFiles = new ArrayList<File>();
      for( File f: compatibilitySources.listFiles() ){
        if( f.isFile() && (f.toString().endsWith(".scala") || f.toString().endsWith(".java")) ){
          compatibilitySourceFiles.add(f);
        }
      }
      changed = compile(changed, start, "", compatibilityTarget, earlyDeps, compatibilitySourceFiles, defaultSecurityManager);
      
      if( classLoaderCache.contains( compatibilityTarget ) ){
        compatibilityClassLoader = classLoaderCache.get( compatibilityTarget );
      } else {
        compatibilityClassLoader = classLoaderCache.put( classLoader(compatibilityTarget, rootClassLoader), compatibilityTarget );
      }
    }

    String[] nailgunClasspathArray = append( earlyDeps.classpathArray, nailgunTarget );
    String nailgunClasspath = classpath( nailgunClasspathArray );
    ClassLoader nailgunClassLoader = new CbtURLClassLoader( new URL[]{}, NailgunLauncher.class.getClassLoader() ); // wrap for caching
    if( !classLoaderCache.contains( nailgunClasspath ) ){
      nailgunClassLoader = classLoaderCache.put( nailgunClassLoader, nailgunClasspath );
    }

    String[] stage1ClasspathArray =
      append( append( nailgunClasspathArray, compatibilityTarget ), stage1Target );
    String stage1Classpath = classpath( stage1ClasspathArray );

    List<File> stage1SourceFiles = new ArrayList<File>();
    for( File f: new File(stage1Sources).listFiles() ){
      if( f.isFile() && f.toString().endsWith(".scala") ){
        stage1SourceFiles.add(f);
      }
    }
    changed = compile(changed, start, stage1Classpath, stage1Target, earlyDeps, stage1SourceFiles, defaultSecurityManager);

    ClassLoader stage1classLoader;
    if( !changed && classLoaderCache.contains( stage1Classpath ) ){
      stage1classLoader = classLoaderCache.get( stage1Classpath );
    } else {
      stage1classLoader =
      classLoaderCache.put(
        classLoader(
          stage1Target,
          new MultiClassLoader2(
            nailgunClassLoader,
            compatibilityClassLoader,
            earlyDeps.classLoader
          )
        ),
        stage1Classpath
      );
    }

    return new BuildStage1Result(
      changed,
      stage1classLoader
    );
  }
}
class BuildStage1Result{
  Boolean changed;
  ClassLoader classLoader;
  BuildStage1Result( Boolean changed, ClassLoader classLoader ){
    this.changed = changed;
    this.classLoader = classLoader;
  }
}
