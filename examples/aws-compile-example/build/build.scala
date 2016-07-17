import cbt._
import cbt.cluster.lambda._
import java.io.File

class Build(val context: Context) extends BaseBuild {
  case class ProjC(implicit val logger: Logger) extends OnAwsLambda (
    new BasicBuild(context.copy(projectDirectory = new File("/home/chav/Code/Scala/ProjC") ))
  ) {
    def bucketName = "testing-for-cbt"
  }

  override def dependencies = super.dependencies ++ 
    Seq(
      new ProjC()
    )
}
