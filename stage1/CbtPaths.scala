package cbt
import java.io._
case class CbtPaths(private val cbtHome: File, private val cache: File){
  val fSep = File.separator 
  val userHome: File = new File(Option(System.getProperty("user.home")).get)
  val nailgun: File = cbtHome ++ (fSep+"nailgun_launcher")
  val stage1: File = cbtHome ++ (fSep+"stage1")
  val stage2: File = cbtHome ++ (fSep+"stage2")
  val mavenCache: File = cache ++ (fSep+"maven")
  private val target = NailgunLauncher.TARGET.stripSuffix(fSep)
  val stage1Target: File = stage1 ++ (fSep ++ target)
  val stage2Target: File = stage2 ++ (fSep ++ target)
  val stage1StatusFile: File = stage1Target ++ ".last-success"
  val stage2StatusFile: File = stage2Target ++ ".last-success"
  val compatibility: File = cbtHome ++ (fSep+"compatibility")
  val nailgunTarget: File = nailgun ++ (fSep ++ target)
  val nailgunStatusFile: File = nailgunTarget ++ ".last-success"
}
