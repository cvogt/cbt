package cbt
import java.io._
object paths{  
  val cbtHome: File = new File(Option(System.getenv("CBT_HOME")).get)
  val mavenCache: File = cbtHome ++ "/cache/maven"
  val userHome: File = new File(Option(System.getProperty("user.home")).get)
  val stage1: File = new File(Option(System.getenv("STAGE1")).get)
  val stage2: File = cbtHome ++ "/stage2"
  val nailgun: File = new File(Option(System.getenv("NAILGUN")).get)
  private val target = Option(System.getenv("TARGET")).get.stripSuffix("/")
  val stage1Target: File = stage1 ++ ("/" ++ target)
  val stage2Target: File = stage2 ++ ("/" ++ target)
  val nailgunTarget: File = nailgun ++ ("/" ++ target)
  val sonatypeLogin: File = cbtHome ++ "/sonatype.login"
}
