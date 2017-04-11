package cbt

import com.trueaccord.scalapb._
import _root_.protocbridge.ProtocBridge
import _root_.scalapb.ScalaPbCodeGenerator
import java.io.File

trait Scalapb extends BaseBuild{
  override def dependencies = super.dependencies ++ Resolver( mavenCentral ).bind(
    ScalaDependency(
      "com.trueaccord.scalapb", "scalapb-runtime", cbt.scalapb.BuildInfo.scalaPBVersion
    )
  )

  def scalapb = Scalapb.apply(lib).config(
    input = projectDirectory / "protobuf-schemas",
    output = projectDirectory / "src_generated"
  )
}

sealed trait ProtocVersion
object ProtocVersion{
  case object v310 extends ProtocVersion
  case object v300 extends ProtocVersion
  case object v261 extends ProtocVersion
  case object v250 extends ProtocVersion
  case object v241 extends ProtocVersion
}

object Scalapb{
  case class apply(lib: Lib){
    case class config(
      input: File,
      output: File,
      version: ProtocVersion = ProtocVersion.v310,
      flatPackage: Boolean = false,
      javaConversions: Boolean = false,
      grpc: Boolean = true,
      singleLineToString: Boolean = false
    ){
      def apply = {
        output.mkdirs
        val protoFiles = input.listRecursive.filter(_.isFile).map(_.string).toArray
        val options = Seq(
          javaConversions.option("java_conversions"),
          grpc.option("grpc"),
          singleLineToString.option("single_line_to_string"),
          flatPackage.option("flat_package")
        ).flatten.mkString(",")
        import _root_.protocbridge.frontend.PluginFrontend
        val pluginFrontend: PluginFrontend = PluginFrontend.newInstance()
        val (scriptPath, state) = pluginFrontend.prepare( ScalaPbCodeGenerator )
        try {
          lib.redirectOutToErr(
            ExitCode(
              com.github.os72.protocjar.Protoc.runProtoc(
                Array(
                  "-" ++ version.getClass.getSimpleName.stripSuffix("$"),
                  "-I=" ++ input.string,
                  s"--scala_out=$options:" ++ output.string,
                  s"--plugin=protoc-gen-scala=${scriptPath}"
                ) ++ protoFiles
              )
            )
          )
        } finally {
          pluginFrontend.cleanup(state)
        }
      }
    }
  }
}
