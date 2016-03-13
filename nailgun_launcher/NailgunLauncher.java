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
  public static ConcurrentHashMap classLoaderCache =
    new ConcurrentHashMap();

  public static SecurityManager defaultSecurityManager = System.getSecurityManager();

  public static void main(String[] args) throws ClassNotFoundException,
                                                NoSuchMethodException,
                                                IllegalAccessException,
                                                InvocationTargetException,
                                                MalformedURLException {
    if (args.length < 3) {
      System.out.println("usage: <main class> <class path> <... args>");
    } else {
      // TODO: cache this classloader, but invalidate on changes
      String[] cp = args[1].split(File.pathSeparator);

      URL[] urls = new URL[cp.length];
      for(int i = 0; i < cp.length; i++){
        urls[i] = new URL("file:"+cp[i]);
      }

      String[] newArgs = new String[args.length-2];
      for(int i = 0; i < args.length-2; i++){
        newArgs[i] = args[i+2];
      }

      new URLClassLoader( urls )
        .loadClass(args[0])
        .getMethod("main", String[].class)
        .invoke( null/* _cls.newInstance()*/, (Object) newArgs );
    }
  }
}
