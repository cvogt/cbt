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
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

/**
 * This file class allows bootstrapping out of Java into Scala. It downloads the Scala jars for the
 * version number given as the first argument into the directory given as the second argument and
 * returns a classpath String.
 */
public class BootstrapScala {

  public final static Dependency[] dependencies(String target, String scalaVersion) throws MalformedURLException {
    return new Dependency[] {
      Dependency.scala(target, scalaVersion, "library", "f75e7acabd57b213d6f61483240286c07213ec0e"),
      Dependency.scala(target, scalaVersion, "compiler","1454c21d39a4d991006a2a47c164f675ea1dafaf"),
      Dependency.scala(target, scalaVersion, "reflect", "bf1649c9d33da945dea502180855b56caf06288c"),
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

    System.out.println(
      String.join(
        File.pathSeparator,
        Arrays.stream(ds).map(d -> d.path.toString()).toArray(String[]::new)
      )
    );

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

