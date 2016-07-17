import cbt._
import java.net.URL
import java.nio._
import java.nio.file.Files._
import java.io.File
import scala.collection.immutable.Seq
import java.util.jar._
import java.util.Enumeration

class Build( context: Context ) extends BasicBuild( context ) with Plugin {
  override def runClass = "CodeDeploy"
  override def dependencies = (
    super.dependencies 
    ++
    Resolver( mavenCentral ).bind(
      MavenDependency( "com.amazonaws", "aws-java-sdk-s3", "1.11.15"),
      MavenDependency( "com.amazonaws", "aws-java-sdk-lambda", "1.11.15"),
      MavenDependency( "com.amazonaws", "aws-java-sdk-core", "1.11.15")
    )
  )
}
