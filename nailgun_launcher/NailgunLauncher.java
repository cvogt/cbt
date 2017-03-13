package cbt;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.security.*;
import java.util.*;
import static cbt.Stage0Lib.*;
import static java.io.File.pathSeparator;
import java.nio.file.*;
import static java.nio.file.Files.write;

/**
 * This launcher allows to start the JVM without loading anything else permanently into its
 * classpath except for the launcher itself. That's why it is written in Java without
 * dependencies outside the JDK.
 */
public class NailgunLauncher{
  /** Persistent cache for caching classloaders for the JVM life time. */
  private static Map<Object,Object> classLoaderCacheHashMap = new HashMap<Object,Object>();

  public final static SecurityManager initialSecurityManager
    = System.getSecurityManager();

  public static String TARGET = System.getenv("TARGET");
  private static String NAILGUN = "nailgun_launcher/";
  private static String STAGE1 = "stage1/";

  @SuppressWarnings("unchecked")
  public static Object getBuild( Object context ) throws Throwable{
    BuildStage1Result res = buildStage1(
      (long) get(context, "cbtLastModified"),
      (long) get(context, "start"),
      ((File) get(context, "cache")).toString() + "/",
      ((File) get(context, "cbtHome")).toString(),
      ((File) get(context, "compatibilityTarget")).toString() + "/",
      new ClassLoaderCache(
        (HashMap) get(context, "persistentCache")
      ),
      (File) get(context, "cwd"),
      (Boolean) get(context, "loop")
    );
    return
      res
        .classLoader
        .loadClass("cbt.Stage1")
        .getMethod( "getBuild", Object.class, BuildStage1Result.class )
        .invoke(null, context, res);
  }

  public static long nailgunLauncherLastModified = -1; // this initial value should be overwritten, never read

  // wrap for caching
  public static ClassLoader jdkClassLoader = new CbtURLClassLoader(
    new URL[]{},
    ClassLoader.getSystemClassLoader().getParent()
  );

  public static List<File> compatibilitySourceFiles;
  public static List<File> nailgunLauncherSourceFiles;
  public static List<File> stage1SourceFiles;

  public static void main( String[] args ) throws Throwable {
    long _start = System.currentTimeMillis();
    if(args[0].equals("check-alive")){
      System.exit(33);
      return;
    }

    System.setSecurityManager( new TrapSecurityManager() );
    installProxySettings();
    String[] diff = args[0].split("\\.");
    long start = _start - (Long.parseLong(diff[0]) * 1000L) - Long.parseLong(diff[1]);

    // if nailgun didn't install it's threadLocal stdout/err replacements, install CBT's.
    // this hack allows to later swap out System.out/err while still affecting things like
    // scala.Console, which captured them at startup
    try{
      System.out.getClass().getDeclaredField("streams"); // nailgun ThreadLocalPrintStream
      assert(System.out.getClass().getName().equals("com.martiansoftware.nailgun.ThreadLocalPrintStream"));
    } catch( NoSuchFieldException e ){
      System.setOut( new PrintStream(new ThreadLocalOutputStream(System.out), true) );
    }
    try{
      System.err.getClass().getDeclaredField("streams"); // nailgun ThreadLocalPrintStream
      assert(System.err.getClass().getName().equals("com.martiansoftware.nailgun.ThreadLocalPrintStream"));
    } catch( NoSuchFieldException e ){
      System.setErr( new PrintStream(new ThreadLocalOutputStream(System.err), true) );
    }
    // ---------------------

    String CBT_HOME = System.getenv("CBT_HOME");
    String cache = CBT_HOME + "/cache/";
    String compatibilityTarget = CBT_HOME + "/compatibility/" + TARGET;
    // copy cache, so that this thread has a consistent view despite other threads
    // changing their copies
    // replace again before returning, see below
    ClassLoaderCache classLoaderCache = new ClassLoaderCache(
      new HashMap<Object,Object>(classLoaderCacheHashMap)
    );

    String nailgunTarget = CBT_HOME + "/" + NAILGUN + TARGET;
    long nailgunLauncherLastModified = new File( nailgunTarget + "../classes.last-success" ).lastModified();

    File cwd = new File(args[1]);
    boolean loop = args[2].equals("0");

    BuildStage1Result res = buildStage1(
      nailgunLauncherLastModified, start, cache, CBT_HOME, compatibilityTarget, classLoaderCache, cwd, loop
    );

    try{
      int exitCode = (int) res
        .classLoader
        .loadClass("cbt.Stage1")
        .getMethod(
          "run", String[].class, File.class, File.class, boolean.class, BuildStage1Result.class, Map.class
        ).invoke(
          null, (Object) args, new File(cache), new File(CBT_HOME), loop, res, classLoaderCache.hashMap
        );

      System.exit( exitCode );
    } catch (java.lang.reflect.InvocationTargetException e) {
      throw unwrapInvocationTargetException(e);
    } finally {
      // This replaces the cache and should be thread-safe.
      // For competing threads the last one wins with a consistent cache.
      // So worst case, we loose some of the cache that's replaced.
      classLoaderCacheHashMap = classLoaderCache.hashMap;
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
    final long lastModified, final long start, final String cache, final String cbtHome,
    final String compatibilityTarget, final ClassLoaderCache classLoaderCache, File cwd, boolean loop
  ) throws Throwable {
    _assert(TARGET != null, "environment variable TARGET not defined");
    String nailgunSources = cbtHome + "/" + NAILGUN;
    String nailgunTarget = nailgunSources + TARGET;
    String stage1Sources = cbtHome + "/" + STAGE1;
    String stage1Target = stage1Sources + TARGET;
    File compatibilitySources = new File(cbtHome + "/compatibility");
    String mavenCache = cache + "maven";
    String mavenUrl = "https://repo1.maven.org/maven2";
    File loopFile = new File(cwd + "/target/.cbt-loop.tmp");
    if(loop){
      loopFile.getParentFile().mkdirs();
    }

    ClassLoader rootClassLoader = new CbtURLClassLoader( new URL[]{}, ClassLoader.getSystemClassLoader().getParent() ); // wrap for caching
    EarlyDependencies earlyDeps = new EarlyDependencies(mavenCache, mavenUrl, classLoaderCache, rootClassLoader);

    nailgunLauncherSourceFiles = new ArrayList<File>();
    for( File f: new File(nailgunSources).listFiles() ){
      if( f.isFile() && f.toString().endsWith(".java") ){
        nailgunLauncherSourceFiles.add(f);
      }
    }

    long nailgunLauncherLastModified = new File( nailgunTarget + "../classes.last-success" ).lastModified();

    long compatibilityLastModified;
    if(!compatibilityTarget.startsWith(cbtHome)){
      compatibilityLastModified = new File( compatibilityTarget + "../classes.last-success" ).lastModified();
    } else {
      compatibilitySourceFiles = new ArrayList<File>();
      for( File f: compatibilitySources.listFiles() ){
        if( f.isFile() && f.toString().endsWith(".java") ){
          compatibilitySourceFiles.add(f);
        }
      }

      if(loop){
        File[] _compatibilitySourceFiles = new File[compatibilitySourceFiles.size()];
        compatibilitySourceFiles.toArray(_compatibilitySourceFiles);
        write(
          loopFile.toPath(),
          (mkString( "\n", _compatibilitySourceFiles ) + "\n").getBytes(),
          StandardOpenOption.CREATE
        );
      }
      compatibilityLastModified = compile( 0L, "", compatibilityTarget, earlyDeps, compatibilitySourceFiles) ;

      if( !classLoaderCache.containsKey( compatibilityTarget, compatibilityLastModified ) ){
        classLoaderCache.put( compatibilityTarget, classLoader(compatibilityTarget, rootClassLoader), compatibilityLastModified );
      }
    }
    final ClassLoader compatibilityClassLoader = classLoaderCache.get( compatibilityTarget, compatibilityLastModified );

    String[] nailgunClasspathArray = append( earlyDeps.classpathArray, nailgunTarget );
    String nailgunClasspath = classpath( nailgunClasspathArray );
    final ClassLoader nailgunClassLoader = new CbtURLClassLoader( new URL[]{}, NailgunLauncher.class.getClassLoader() ); // wrap for caching
    if( !classLoaderCache.containsKey( nailgunClasspath, nailgunLauncherLastModified ) ){
      classLoaderCache.put( nailgunClasspath, nailgunClassLoader, nailgunLauncherLastModified );
    }

    String[] stage1ClasspathArray =
      append( append( nailgunClasspathArray, compatibilityTarget ), stage1Target );
    String stage1Classpath = classpath( stage1ClasspathArray );

    stage1SourceFiles = new ArrayList<File>();
    for( File f: new File(stage1Sources).listFiles() ){
      if( f.isFile() && f.toString().endsWith(".scala") ){
        stage1SourceFiles.add(f);
      }
    }

    final long stage1BeforeCompiled = System.currentTimeMillis();
    final long stage0LastModified = Math.max(
      lastModified,
      Math.max( lastModified, compatibilityLastModified )
    );

    if(loop){
      File[] _stage1SourceFiles = new File[stage1SourceFiles.size()];
      stage1SourceFiles.toArray(_stage1SourceFiles);
      write(
        loopFile.toPath(),
        (mkString( "\n", _stage1SourceFiles ) + "\n").getBytes(),
        StandardOpenOption.APPEND
      );
    }

    final long stage1LastModified = compile(
      stage0LastModified, stage1Classpath, stage1Target, earlyDeps, stage1SourceFiles
    );

    if( stage1LastModified < compatibilityLastModified )
      throw new AssertionError(
        "Cache invalidation bug: cbt compatibility layer recompiled, but cbt stage1 did not."
      );

    if( !classLoaderCache.containsKey( stage1Classpath, stage1LastModified ) ){
      classLoaderCache.put(
        stage1Classpath,
        classLoader(
          stage1Target,
          new MultiClassLoader2(
            nailgunClassLoader,
            compatibilityClassLoader,
            earlyDeps.classLoader
          )
        ),
        stage1LastModified
      );
    }
    final ClassLoader stage1classLoader = classLoaderCache.get( stage1Classpath, stage1LastModified );

    return new BuildStage1Result(
      start,
      stage1LastModified,
      stage1classLoader,
      stage1Classpath,
      nailgunClasspath,
      compatibilityTarget
    );
  }
}
