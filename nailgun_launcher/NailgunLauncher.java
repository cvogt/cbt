package cbt;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.*;
import java.nio.file.*;
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

  /**
   * Persistent cache for caching classloaders for the JVM life time. Can be used as needed by user
   * code to improve startup time.
   */
  public static ConcurrentHashMap<String, Object> classLoaderCacheKeys = new ConcurrentHashMap<String,Object>();
  public static ConcurrentHashMap<Object, ClassLoader> classLoaderCacheValues = new ConcurrentHashMap<Object,ClassLoader>();

  public static SecurityManager defaultSecurityManager = System.getSecurityManager();

  public static String CBT_HOME = System.getenv("CBT_HOME");
  public static String NAILGUN = System.getenv("NAILGUN");
  public static String STAGE1 = CBT_HOME + "/stage1/";
  public static String TARGET = System.getenv("TARGET");

  public static String SCALA_VERSION = "2.11.8";

  public static void _assert(Boolean condition, Object msg){
    if(!condition){
      throw new AssertionError("Assertion failed: "+msg);
    }
  }

  public static void main(String[] args) throws ClassNotFoundException,
                                                NoSuchMethodException,
                                                IllegalAccessException,
                                                InvocationTargetException,
                                                MalformedURLException,
                                                IOException,
                                                NoSuchAlgorithmException {
    _assert(CBT_HOME != null, CBT_HOME);
    _assert(NAILGUN != null, NAILGUN);
    _assert(TARGET != null, TARGET);
    _assert(STAGE1 != null, STAGE1);

    File f2 = new File(STAGE1);
    _assert(f2.listFiles() != null, f2);
    long lastCompiled = new File(STAGE1 + TARGET + "/cbt/Stage1.class").lastModified();
    for( File file: f2.listFiles() ){
      if( file.isFile() && file.toString().endsWith(".scala")
          && file.lastModified() > lastCompiled ){

        EarlyDependency[] dependencies = new EarlyDependency[]{
          EarlyDependency.scala("library", "DDD5A8BCED249BEDD86FB4578A39B9FB71480573"),
          EarlyDependency.scala("compiler","FE1285C9F7B58954C5EF6D80B59063569C065E9A"),
          EarlyDependency.scala("reflect", "B74530DEEBA742AB4F3134DE0C2DA0EDC49CA361"),
          new EarlyDependency("org/scala-lang/modules/scala-xml_2.11/1.0.5", "scala-xml_2.11-1.0.5", "77ac9be4033768cf03cc04fbd1fc5e5711de2459")
        };

        ArrayList<String> scalaClassPath = new ArrayList<String>();

        for (EarlyDependency d: dependencies) {
          download( d.url, d.path, d.hash );
          scalaClassPath.add( d.path.toString() );
        }

        File stage1ClassFiles = new File(STAGE1 + TARGET + "/cbt/");
        if( stage1ClassFiles.exists() ){
          for( File f: stage1ClassFiles.listFiles() ){
          if( f.toString().endsWith(".class") ){
            f.delete();
          }
        }
        }

        new File(STAGE1 + TARGET).mkdirs();

        String s = File.pathSeparator;
        ArrayList<String> scalacArgsList = new ArrayList<String>(
          Arrays.asList(
            new String[]{
              "-deprecation", "-feature",
              "-cp", String.join( s, scalaClassPath.toArray(new String[scalaClassPath.size()])) + s + NAILGUN+TARGET,
              "-d", STAGE1+TARGET
            }
          )
        );

        for( File f: new File(STAGE1).listFiles() ){
          if( f.isFile() && f.toString().endsWith(".scala") ){
            scalacArgsList.add( f.toString() );
          }
        }

        ArrayList<URL> urls = new ArrayList<URL>();
        for( String c: scalaClassPath ){
          urls.add(new URL("file:"+c));
        }
        ClassLoader cl = new CbtURLClassLoader( (URL[]) urls.toArray(new URL[urls.size()]) );
        cl.loadClass("scala.tools.nsc.Main")
          .getMethod("main", String[].class)
          .invoke( null/* _cls.newInstance()*/, (Object) scalacArgsList.toArray(new String[scalacArgsList.size()]));
        break;
      }
    }

    String library = CBT_HOME+"/cache/maven/org/scala-lang/scala-library/"+SCALA_VERSION+"/scala-library-"+SCALA_VERSION+".jar";
    if(!classLoaderCacheKeys.containsKey(library)){
      Object libraryKey = new Object();
      classLoaderCacheKeys.put(library,libraryKey);
      ClassLoader libraryClassLoader = new CbtURLClassLoader( new URL[]{ new URL("file:"+library) } );
      classLoaderCacheValues.put(libraryKey, libraryClassLoader);

      String xml = CBT_HOME+"/cache/maven/org/scala-lang/modules/scala-xml_2.11/1.0.5/scala-xml_2.11-1.0.5.jar";
      Object xmlKey = new Object();
      classLoaderCacheKeys.put(xml,xmlKey);
      ClassLoader xmlClassLoader = new CbtURLClassLoader(
        new URL[]{ new URL("file:"+xml) },
        libraryClassLoader
      );
      classLoaderCacheValues.put(xmlKey, xmlClassLoader);

      Object nailgunKey = new Object();
      classLoaderCacheKeys.put(NAILGUN+TARGET,nailgunKey);
      ClassLoader nailgunClassLoader = new CbtURLClassLoader(
        new URL[]{ new URL("file:"+NAILGUN+TARGET) },
        xmlClassLoader
      );
      classLoaderCacheValues.put(nailgunKey, nailgunClassLoader);
    }

    if(args[0].equals("check-alive")){
      System.exit(33);
      return;
    }

    ClassLoader cl = new CbtURLClassLoader(
      new URL[]{ new URL("file:"+STAGE1+TARGET) }, 
      classLoaderCacheValues.get(
        classLoaderCacheKeys.get( NAILGUN+TARGET )
      )
    );

    try{    
      cl.loadClass("cbt.Stage1")
        .getMethod("main", String[].class, ClassLoader.class)
        .invoke( null/* _cls.newInstance()*/, (Object) args, cl);
    }catch(ClassNotFoundException e){
      System.err.println(cl);
      throw e;
    }catch(NoClassDefFoundError e){
      System.err.println(cl);
      throw e;
    }catch(InvocationTargetException e){
      System.err.println(cl);
      throw e;
    }
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
}


