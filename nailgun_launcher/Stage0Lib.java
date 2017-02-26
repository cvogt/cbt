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
import static java.lang.Math.min;

public class Stage0Lib{
  public static void _assert(boolean condition, Object msg){
    if(!condition){
      throw new AssertionError("Assertion failed: "+msg);
    }
  }

  public static int runMain(String cls, String[] args, ClassLoader cl) throws Throwable{
    boolean trapExitCodeBefore = TrapSecurityManager.trapExitCode().get();
    try{
      TrapSecurityManager.trapExitCode().set(true);
      cl.loadClass(cls)
        .getMethod("main", String[].class)
        .invoke( null, (Object) args);
      return 0;
    }catch( InvocationTargetException exception ){
      Throwable cause = exception.getCause();
      if(TrapSecurityManager.isTrappedExit(cause)){
        return TrapSecurityManager.exitCode(cause);
      }
      throw exception;
    } finally {
      TrapSecurityManager.trapExitCode().set(trapExitCodeBefore);
    }
  }

  public static Object get(Object object, String method) throws Throwable{
    return object.getClass().getMethod( method ).invoke(object);
  }

  public static String classpath( String... files ){
    Arrays.sort(files);
    return mkString( pathSeparator, files );
  }

  public static long lastModified( String... files ){
    List<Long> lastModified = new ArrayList<Long>();
    for( String file: files ){
      lastModified.add( new File(file).lastModified() );
    }
    return Collections.max( lastModified );
  }

  public static ClassLoader loadDependency(
    String url,
    String file,
    String hash,
    ClassLoaderCache classLoaderCache,
    ClassLoader parent,
    String... classpathArray
  ) throws Throwable {
    download(new URL(url), Paths.get(file), hash);

    final long lastModified = lastModified( classpathArray );
    final String classpath = classpath( classpathArray );

    if( !classLoaderCache.containsKey( classpath, lastModified ) )
      classLoaderCache.put( classpath, classLoader( file, parent ), lastModified );

    return classLoaderCache.get( classpath, lastModified );
  }

  public static File write(File file, String content, OpenOption... options) throws Throwable{
    file.getParentFile().mkdirs();
    Files.write(file.toPath(), content.getBytes(), options);
    return file;
  }

  public static long compile(
    long lastModified, String classpath, String target,
    EarlyDependencies earlyDeps, List<File> sourceFiles
  ) throws Throwable{
    File statusFile = new File( new File(target) + ".last-success" );
    long lastCompiled = statusFile.lastModified();

    long maxLastModified = lastModified;
    final long start = System.currentTimeMillis(); // <- before recursing, so we catch them all

    for( File file: sourceFiles ){
      long l = file.lastModified();
      if( l > maxLastModified ) maxLastModified = l;
      // performance optimization because we'll recompile and don't need to check other files
      if( l > lastCompiled ) break;
    }

    if( maxLastModified > lastCompiled ){
      List<String> zincArgs = new ArrayList<String>(
        Arrays.asList(
          new String[]{
            "-scala-compiler", earlyDeps.scalaCompiler_2_11_8_File,
            "-scala-library", earlyDeps.scalaLibrary_2_11_8_File,
            "-scala-extra", earlyDeps.scalaReflect_2_11_8_File,
            "-sbt-interface", earlyDeps.sbtInterface_0_13_13_File,
            "-compiler-interface", earlyDeps.compilerInterface_0_13_13_File,
            "-cp", classpath,
            "-d", target,
            "-S-deprecation",
            "-S-feature",
            "-S-unchecked"
          }
        )
      );

      for( File f: sourceFiles ){
        zincArgs.add(f.toString());
      }

      PrintStream oldOut = System.out;
      try{
        System.setOut(System.err);
        int exitCode = runMain( "com.typesafe.zinc.Main", zincArgs.toArray(new String[zincArgs.size()]), earlyDeps.zinc );
        if( exitCode == 0 ){
          write( statusFile, "" );
          Files.setLastModifiedTime( statusFile.toPath(), FileTime.fromMillis(start) );
        } else {
          System.exit( exitCode );
        }
      } finally {
        System.setOut(oldOut);
      }
      return statusFile.lastModified(); // can't just use `start` here as system time precision is less than milliseconds on OSX
    } else {
      return lastCompiled;
    }
  }

  public static ClassLoader classLoader( String file ) throws Throwable{
    return new CbtURLClassLoader(
      new URL[]{ new URL("file:"+file) }
    );
  }
  public static ClassLoader classLoader( String file, ClassLoader parent ) throws Throwable{
    return new CbtURLClassLoader(
      new URL[]{ new URL("file:"+file) }, parent
    );
  }

  private static String getVarFromEnv(String envKey) {
    String value = System.getenv(envKey);
    if(value == null || value.isEmpty()) {
      value = System.getenv(envKey.toUpperCase());
    }
    return value;
  }

  private static void setProxyfromPropOrEnv(String envKey, String propKeyH, String propKeyP) {
    String proxyHost = System.getProperty(propKeyH);
    String proxyPort = System.getProperty(propKeyP);
    if((proxyHost == null || proxyHost.isEmpty()) && (proxyPort == null || proxyPort.isEmpty())) {
      String envVar = getVarFromEnv(envKey);
      if(envVar != null && !envVar.isEmpty()) {
        String[] proxy = envVar.replaceFirst("^https?://", "").split(":", 2);
        System.setProperty(propKeyH, proxy[0]);
        System.setProperty(propKeyP, proxy[1]);
      }
    }
  }

  public static void installProxySettings() throws URISyntaxException {
    setProxyfromPropOrEnv("http_proxy", "http.proxyHost", "http.proxyPort");
    setProxyfromPropOrEnv("https_proxy", "https.proxyHost", "https.proxyPort");
    String nonHosts = System.getProperty("http.nonProxyHosts");
    if(nonHosts == null || nonHosts.isEmpty()) {
      String envVar = getVarFromEnv("no_proxy");
      if(envVar != null && !envVar.isEmpty()) {
        System.setProperty("http.nonProxyHosts", envVar.replaceAll(",","|"));
      }
    }
  }

  private static final ProxySelector ps = ProxySelector.getDefault();

  public static HttpURLConnection openConnectionConsideringProxy(URL urlString)
      throws IOException, URISyntaxException {
    java.net.Proxy proxy = ps.select(urlString.toURI()).get(0);
    return (HttpURLConnection) urlString.openConnection(proxy);
  }

  public static void download(URL urlString, Path target, String sha1) throws Throwable {
    final Path unverified = Paths.get(target+".unverified");
    if(!Files.exists(target)) {
      new File(target.toString()).getParentFile().mkdirs();
      System.err.println("downloading " + urlString);
      System.err.println("to " + target);
      final InputStream stream = openConnectionConsideringProxy(urlString).getInputStream();
      Files.copy(stream, unverified, StandardCopyOption.REPLACE_EXISTING);
      stream.close();
      final String checksum = sha1(Files.readAllBytes(unverified));
      if(sha1 == null || sha1.toLowerCase().equals(checksum)) {
        Files.move(unverified, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } else {
        System.err.println(target + " checksum does not match.\nExpected: |" + sha1 + "|\nFound:    |" + checksum + "|");
        System.exit(1);
      }
    }
  }

  public static String sha1(byte[] bytes) throws Throwable {
    final MessageDigest sha1 = MessageDigest.getInstance("SHA1");
    sha1.update(bytes, 0, bytes.length);
    return (new HexBinaryAdapter()).marshal(sha1.digest()).toLowerCase();
  }

  public static String mkString(String separator, Object[] parts){
    String result = parts[0].toString();
    for(int i = 1; i < parts.length; i++){
      result += separator + parts[i].toString();
    }
    return result;
  }

  public static String[] append( String[] array, String item ){
    String[] copy = Arrays.copyOf(array, array.length + 1);
    copy[array.length] = item;
    return copy;
  }
}
