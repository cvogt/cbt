package cbt
import java.nio.file._
case class CbtPaths(private val cbtHome: Path, private val cache: Path){
  val userHome: Path = Paths.get(Option(System.getProperty("user.home")).get)
  val nailgun: Path = cbtHome ++ "nailgun_launcher"
  val stage1: Path = cbtHome ++ "stage1"
  val stage2: Path = cbtHome ++ "stage2"
  val mavenCache: Path = cache ++ "/maven"
  private val target = NailgunLauncher.TARGET.stripSuffix("/")
  val stage1Target: Path = stage1 ++ target
  val stage2Target: Path = stage2 ++ target
  val stage2StatusFile: Path = Paths.get(stage2Target.toString.stripSuffix("/") + ".last-success")
  val compatibility: Path = cbtHome ++ "compatibility"
  val nailgunTarget: Path = nailgun ++ target
}
