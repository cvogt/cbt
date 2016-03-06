import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

class Dependency {

  final URL    url;
  final Path   path;
  final String hash;

  public Dependency(String target, String folder, String file, String hash) throws MalformedURLException {
    this.path = Paths.get(target + file + ".jar");
    this.url  = new URL("https://repo1.maven.org/maven2/org/scala-lang/" + folder + "/" + file + ".jar");
    this.hash = hash;
  }

  // scala-lang dependency
  public static Dependency scala(String target, String scalaVersion, String scalaModule, String hash) 
    throws MalformedURLException {
    return new Dependency(
      target,
      "scala-" + scalaModule + "/" + scalaVersion,
      "scala-" + scalaModule + "-" + scalaVersion,
      hash
    );
  }

}
