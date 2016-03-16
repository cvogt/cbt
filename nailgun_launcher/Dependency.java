package cbt;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

class EarlyDependency {

  final URL    url;
  final Path   path;
  final String hash;

  public EarlyDependency(String folder, String file, String hash) throws MalformedURLException {
    this.path = Paths.get(NailgunLauncher.CBT_HOME + "/cache/maven/" + folder + "/" + file + ".jar");
    this.url  = new URL("https://repo1.maven.org/maven2/" + folder + "/" + file + ".jar");
    this.hash = hash;
  }

  // scala-lang dependency
  public static EarlyDependency scala(String scalaModule, String hash) 
    throws MalformedURLException {
    return new EarlyDependency(
      "org/scala-lang/scala-" + scalaModule + "/" + NailgunLauncher.SCALA_VERSION,
      "scala-" + scalaModule + "-" + NailgunLauncher.SCALA_VERSION,
      hash
    );
  }

}
