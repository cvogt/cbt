import cbt._
import cbt.cluster.lambda._
class Build(val context: Context) extends BaseBuild {
  override def dependencies = super.dependencies
  override def run = {
    AwsLambdaLib.deployWithCbt("None", context.projectDirectory)
    cbt.ExitCode.Success
  }
}
