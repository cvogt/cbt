import cbt._
import cbt.cluster.lambda._
import java.io.File

class Build(val context: Context) extends BaseBuild {
  case class ProjC(implicit val logger: Logger) extends OnAwsLambda (
    new BasicBuild(context.copy(projectDirectory = new File("/home/chav/Code/Scala/ProjC") ))
  ) {
    def bucketName = "test-bucket-chav"
  }

  override def dependencies = super.dependencies ++ 
    Seq(
      new ProjC()
    )

  override def run = {
    AwsLambdaLib.deployWithCbt("None", context.projectDirectory)
    cbt.ExitCode.Success
  }
}
