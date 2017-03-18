package cbt

import java.io.File
import java.util.Date
import scala.io.Codec
import com.typesafe.config.ConfigFactory

import org.scalastyle.MainConfig
import org.scalastyle.Directory
import org.scalastyle.ScalastyleConfiguration
import org.scalastyle.ScalastyleChecker
import org.scalastyle.{XmlOutput, TextOutput}

trait Scalastyle extends BaseBuild {
  def scalastyle = {
      val dirList = List(projectDirectory) map (_.getAbsolutePath)

	    val result = ScalaStyle(dirList, ScalaStyle.checkConfig(projectDirectory.getAbsolutePath)) 
      result match {
        case 1 => println("Error Scalastyle Checking")
        case 2 => println("Config file for ScalaStyle not found")
        case _ => print("")
      }
  }
}


object ScalaStyle {

  val defaultConfigName : String = "/scalastyle_config.xml"

  def userHome = Option( System.getProperty("user.home") )

  def checkConfig(directory: String, fallback: Option[String] = userHome) : Option[String] = {
    
    def checkConfigExists(filename: String) : Option[String] = {
      val file = new File(filename)
      if (file.exists()) Some(filename) else None
    }

    checkConfigExists(directory + defaultConfigName) orElse (
      fallback flatMap (fdir => checkConfigExists(fdir + defaultConfigName))
    )
  }



  def apply(directories: List[String], configFile: Option[String]) : Int = {
    val exitVal = configFile match {
      case Some(_) => {
         val conf = MainConfig(false).copy(config = configFile, directories = directories)
         if (conf.error) {
            1
         } else {
            if (execute(conf)) 1 else 0
         }
      }
      case None => {
        2
      } 
    }
   
    exitVal
  }


  private[this] def now(): Long = new Date().getTime()

  private[this] def execute(mc: MainConfig)(implicit codec: Codec): Boolean = {
    val start = now()
    val configuration = ScalastyleConfiguration.readFromXml(mc.config.get)
    val cl = mc.externalJar.flatMap(j => Some(new java.net.URLClassLoader(Array(new java.io.File(j).toURI().toURL()))))
    val messages = new ScalastyleChecker(cl).checkFiles(configuration, Directory.getFiles(mc.inputEncoding, mc.directories.map(new File(_)).toSeq, excludedFiles=mc.excludedFiles))

    // scalastyle:off regex
    val config = ConfigFactory.load(cl.getOrElse(this.getClass().getClassLoader()))
    val outputResult = new TextOutput(config, mc.verbose, mc.quiet).output(messages)
    mc.xmlFile match {
      case Some(x) => {
        val encoding = mc.xmlEncoding.getOrElse(codec.charSet).toString
        XmlOutput.save(config, x, encoding, messages)
      }
      case None =>
    }

    if (!mc.quiet) println("Processed " + outputResult.files + " file(s)")
    if (!mc.quiet) println("Found " + outputResult.errors + " errors")
    if (!mc.quiet) println("Found " + outputResult.warnings + " warnings")
    if (!mc.quiet) println("Finished in " + (now - start) + " ms")

    // scalastyle:on regex

    outputResult.errors > 0 || (mc.warningsaserrors && outputResult.warnings > 0)
  }
  
}