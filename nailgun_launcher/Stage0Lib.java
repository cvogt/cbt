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

  public static int runMain(String cls, String[] args, ClassLoader cl) throws Throwable{
    Boolean trapExitCodeBefore = TrapSecurityManager.trapExitCode().get();
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
    return join( pathSeparator, files );
  }

  public static File write(File file, String content, OpenOption... options) throws Throwable{
    file.getParentFile().mkdirs();
    Files.write(file.toPath(), content.getBytes(), options);
    return file;
  }

  public static Boolean compile(
    Boolean changed, Long start, String classpath, String target,
    EarlyDependencies earlyDeps, List<File> sourceFiles
  ) throws Throwable{
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
            "-sbt-interface", earlyDeps.sbtInterface_0_13_12_File,
            "-compiler-interface", earlyDeps.compilerInterface_0_13_12_File,
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
    }
    return changed;
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
    if(value==null || value.isEmpty()) {
      value = System.getenv(envKey.toUpperCase());
    }
    return value;
  }

  private static void setProxyfromPropOrEnv(String envKey, String propKeyH, String propKeyP) {
    String proxyHost = System.getProperty(propKeyH);
    String proxyPort = System.getProperty(propKeyP);
    if((proxyHost==null || proxyHost.isEmpty()) && (proxyPort==null || proxyPort.isEmpty())) {
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
    if(nonHosts==null || nonHosts.isEmpty()) {
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
