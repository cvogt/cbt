package cbt
import java.io._
object paths{  
  val cbtHome = new File(Option(System.getenv("CBT_HOME")).get)
  val mavenCache = new File(cbtHome+"/cache/maven/")
  val userHome = new File(Option(System.getProperty("user.home")).get)
  val stage1 = new File(Option(System.getenv("STAGE1")).get)
  val stage2 = new File(cbtHome + "/stage2/")
  val nailgun = new File(Option(System.getenv("NAILGUN")).get)
  private val target = Option(System.getenv("TARGET")).get
  val stage1Target = new File(stage1 + "/" + target)
  val stage2Target = new File(stage2 + "/" + target)
  val nailgunTarget = new File(nailgun + "/" + target)
  val sonatypeLogin = new File(cbtHome+"/sonatype.login")
}
