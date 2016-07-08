import cbt._
import java.net.URL
import java.nio._
import java.nio.file.Files._
import java.io.File
import scala.collection.immutable.Seq
import java.util.jar._
import java.util.Enumeration

class Build( context: Context ) extends BasicBuild( context ) with Plugin {
  override def runClass = "Deploy" // make runCbt to test locally
  override def dependencies = (
    super.dependencies // don't forget super.dependencies here
    ++
    Resolver( mavenCentral ).bind(
      MavenDependency( "com.amazonaws", "aws-java-sdk", "1.11.8" ),
      MavenDependency( "com.amazonaws", "aws-lambda-java-core", "1.1.0"),
      MavenDependency( "com.amazonaws", "aws-java-sdk-s3", "1.11.9"),
      MavenDependency( "com.amazonaws", "aws-java-sdk-events", "1.11.9" ),
      MavenDependency( "com.amazonaws", "aws-lambda-java-events", "1.3.0"),
      MavenDependency( "com.google.jimfs", "jimfs", "1.1")
    )
  )
}
