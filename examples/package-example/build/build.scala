package package_example_build
import java.nio.file._
import java.security.MessageDigest
import cbt._
class Build(val context: Context) extends BaseBuild with PackageJars {
  override def groupId = "org.example"
  override def version = "1.0.0"

  def hash(file: Path): String = {
    val digest = MessageDigest.getInstance("SHA-256");
    val in = Files.newInputStream(file)
    try {
      val chunk = new Array[Byte](4096)
      while(in.read(chunk) > 0) {
        digest.update(chunk)
      }
    } finally {
      in.close
    }
    val bytes = digest.digest()
    val builder = new StringBuilder(bytes.length * 2);
    for(b <- bytes) {
      builder.append(f"$b%02x")
    }
    builder.toString
  }

  override def run = {
    val pass1 = super.`package`.map(f => hash(f.toPath))
    lib.clean(cleanFiles, true, false, false, false)
    transientCache.clear()
    val pass2 = super.`package`.map(f => hash(f.toPath))

    assert(
      pass1 == pass2,
      "The checksums of jars generated during two separate builds did not match. " +
        "The build is not reproducible."
    )
    ExitCode.Success
  }

}
