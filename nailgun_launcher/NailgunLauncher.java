package cbt;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This launcher allows to start the JVM without loading anything else permanently into its
 * classpath except for the launcher itself. That's why it is written in Java without
 * dependencies outside the JDK.
 *
 * The main method loads the given class from the given class path, calls it's main
 * methods passing in the additional arguments.
 */
public class NailgunLauncher{

  /**
   * Persistent cache for caching classloaders for the JVM life time. Can be used as needed by user
   * code to improve startup time.
   */
  public static ConcurrentHashMap<String, Object> classLoaderCacheKeys = new ConcurrentHashMap<String,Object>();
  public static ConcurrentHashMap<Object, ClassLoader> classLoaderCacheValues = new ConcurrentHashMap<Object,ClassLoader>();

  public static SecurityManager defaultSecurityManager = System.getSecurityManager();

  public static void main(String[] args) throws ClassNotFoundException,
                                                NoSuchMethodException,
                                                IllegalAccessException,
                                                InvocationTargetException,
                                                MalformedURLException {
    String CBT_HOME = System.getenv("CBT_HOME");
    String SCALA_VERSION = System.getenv("SCALA_VERSION");
    String NAILGUN = System.getenv("NAILGUN");
    String STAGE1 = System.getenv("STAGE1");
    String TARGET = System.getenv("TARGET");
    assert(CBT_HOME != null);
    assert(SCALA_VERSION != null);
    assert(NAILGUN != null);
    assert(STAGE1 != null);
    assert(TARGET != null);

    String library = CBT_HOME+"/bootstrap_scala/cache/"+SCALA_VERSION+"/scala-library-"+SCALA_VERSION+".jar";
    if(!classLoaderCacheKeys.containsKey(library)){
      Object libraryKey = new Object();
      classLoaderCacheKeys.put(library,libraryKey);
      ClassLoader libraryClassLoader = new URLClassLoader( new URL[]{ new URL("file:"+library) } );
      classLoaderCacheValues.put(libraryKey, libraryClassLoader);

      String xml = CBT_HOME+"/bootstrap_scala/cache/"+SCALA_VERSION+"/scala-xml_2.11-1.0.5.jar";
      Object xmlKey = new Object();
      classLoaderCacheKeys.put(xml,xmlKey);
      ClassLoader xmlClassLoader = new URLClassLoader(
        new URL[]{ new URL("file:"+xml) },
        libraryClassLoader
      );
      classLoaderCacheValues.put(xmlKey, xmlClassLoader);

      Object nailgunKey = new Object();
      classLoaderCacheKeys.put(NAILGUN+TARGET,nailgunKey);
      ClassLoader nailgunClassLoader = new URLClassLoader(
        new URL[]{ new URL("file:"+NAILGUN+TARGET) },
        xmlClassLoader
      );
      classLoaderCacheValues.put(nailgunKey, nailgunClassLoader);
    }

    if(args[0].equals("check-alive")){
      System.exit(33);
      return;
    }

    ClassLoader cl = new URLClassLoader(
      new URL[]{ new URL("file:"+STAGE1+TARGET) }, 
      classLoaderCacheValues.get(
        classLoaderCacheKeys.get( NAILGUN+TARGET )
      )
    );

    cl.loadClass("cbt.Stage1")
      .getMethod("main", String[].class, ClassLoader.class)
      .invoke( null/* _cls.newInstance()*/, (Object) args, cl);
  }
}

/*
protected class MyURLClassLoader extends URLClassLoader{
  public String toString(){
    return super.toString() + "(\n  " + Arrays.toString(urls) + "\n)";
  }
}
*/
