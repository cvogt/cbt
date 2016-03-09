import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

/**
 * This file class allows bootstrapping out of Java into Scala. It downloads the Scala jars for the
 * version number given as the first argument into the directory given as the second argument and
 * returns a classpath String.
 */
public class BootstrapScala {

  public final static Dependency[] dependencies(String target, String scalaVersion) throws MalformedURLException {
    return new Dependency[] {
      Dependency.scala(target, scalaVersion, "library", "DDD5A8BCED249BEDD86FB4578A39B9FB71480573"),
      Dependency.scala(target, scalaVersion, "compiler","FE1285C9F7B58954C5EF6D80B59063569C065E9A"),
      Dependency.scala(target, scalaVersion, "reflect", "B74530DEEBA742AB4F3134DE0C2DA0EDC49CA361"),
      new Dependency(target, "modules/scala-xml_2.11/1.0.5", "scala-xml_2.11-1.0.5", "77ac9be4033768cf03cc04fbd1fc5e5711de2459")
    };
  }

  public static void main(String args[]) throws IOException, NoSuchAlgorithmException {

    if(args.length < 2){
      System.err.println("Usage: bootstrap_scala <scala version> <download directory>");
      System.exit(1);
    }

    Dependency[] ds = dependencies( args[1], args[0] );
    new File(args[1]).mkdirs();
    for (Dependency d: ds) {
      download( d.url, d.path, d.hash );
    }

    // Join dep. paths as a classpath
    String classpath = "";
    Iterator<Dependency> depsIter = Arrays.asList(ds).iterator();
    while (depsIter.hasNext()) {
      Dependency dep = depsIter.next();
      classpath += dep.path.toString();
      if (depsIter.hasNext()) {
          classpath += File.pathSeparator;
      }
    }

    System.out.println(classpath);

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
