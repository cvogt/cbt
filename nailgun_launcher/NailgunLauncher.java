package cbt;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/*
This launcher allows to use Nailgun without loading anything else permanenetly into it's classpath.
The main method loads the given class from the given class math, calls it's main methods passing
in the additional arguments.
*/
public class NailgunLauncher{
	/* Persistent cache for caching classloaders for the JVM life time.
	   Can be used as needed by user code to improve startup time. */
	public static ConcurrentHashMap<String,ClassLoader> classLoaderCache = new ConcurrentHashMap<String,ClassLoader>();
	public static void main(String[] args){
		try{
			if(args.length < 3){
				System.out.println("usage: <main class> <class path> <... args>");
			} else {
				URLClassLoader cl = new URLClassLoader( // TODO: cache this classloader, but invalidate on changes
					Arrays.stream(
						args[1].split(File.pathSeparator)
					).filter( cp -> !(cp == "") ).map( cp -> {
						try { return new URL("file:" + cp); }
						catch(MalformedURLException e) { throw new RuntimeException(e); }
					}).toArray(URL[]::new)
				){
					public String toString(){
						String suffix = "";
						if(getParent() != ClassLoader.getSystemClassLoader())
							suffix = ", "+getParent();
						return "URLClassLoader(" + Arrays.toString(getURLs()) + suffix +")";
					}
				};
				cl  .loadClass( args[0] )
					.getMethod( "main", String[].class )
					.invoke(
						null/* _cls.newInstance()*/,
						(Object) Arrays.stream(args).skip(2).toArray( String[]::new )
					);
			}
		} catch (Exception e) { throw new RuntimeException(e); }
	}
}
