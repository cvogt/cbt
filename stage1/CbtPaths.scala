package cbt
import java.io._
case class CbtPaths(private val cbtHome: File, private val cache: File){
  val userHome: File = new File(Option(System.getProperty("user.home")).get)
  val nailgun: File = cbtHome ++ "/nailgun_launcher"
  val stage1: File = cbtHome ++ "/stage1"
  val stage2: File = cbtHome ++ "/stage2"
  val mavenCache: File = cache ++ "/maven"
  private val target = NailgunLauncher.TARGET.stripSuffix("/")
  val stage1Target: File = stage1 ++ ("/" ++ target)
  val stage2Target: File = stage2 ++ ("/" ++ target)
  val stage2StatusFile: File = stage2Target ++ ".last-success"
  val compatibility: File = cbtHome ++ "/compatibility"
  val nailgunTarget: File = nailgun ++ ("/" ++ target)
}
