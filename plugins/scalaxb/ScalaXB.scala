package cbt

import java.io.File
import scalaxb.{compiler => sc}
import scalaxb.compiler.{Config => ScConfig}
import sc.ConfigEntry._
import cbt.ScalaXB._

trait ScalaXB extends BaseBuild {
  private val targetDirectory = projectDirectory / "target"

  def generateScalaXBSources(outputDirectory: File = (targetDirectory / "src_managed"), verbose: Boolean = false) =
    ScalaXB.GenerateScalaXBSources(sources, projectDirectory, outputDirectory, verbose)
}

object ScalaXB {
  case class GenerateScalaXBSources(sources: Seq[File], projectRoot: File, outputDirectory: File, verbose: Boolean = false) {
    def defaultConfig: Config =
      Config(
        xsdSourceDir = projectRoot / "src" / "main" / "xsd",
        wsdlSourceDir = projectRoot / "src" / "main" / "wsdl",
        generatedPackageName = "generated",
        generatedPackageNames = Map(),
        autoPackages = false,
        classPrefix = None,
        paramPrefix = None,
        attributePrefix = None,
        prependFamily = false,
        wrapContents = Nil,
        contentSizeLimit = Int.MaxValue,
        chunkSize = 10,
        namedAttributes = false,
        packageDir = true,
        generateRuntime = true,
        generateClients = List(Dispatch(ScConfig.defaultDispatchVersion.value), Async),
        ignoreUnknown = false,
        vararg = false,
        generateVisitor = false,
        generateMutable = false,
        protocolFileName = sc.Defaults.protocolFileName,
        protocolPackageName = None,
        laxAny = false,
        additionalCombinedPackageNames = Map()
      )

    case class Config (
      xsdSourceDir: File,
      wsdlSourceDir: File,
      generatedPackageName: String,
      generatedPackageNames: Map[String, String],
      autoPackages: Boolean,
      classPrefix: Option[String],
      paramPrefix: Option[String],
      attributePrefix: Option[String],
      prependFamily: Boolean,
      wrapContents: List[String],
      contentSizeLimit: Int,
      chunkSize: Int,
      namedAttributes: Boolean,
      packageDir: Boolean,
      generateRuntime: Boolean,
      protocolFileName: String,
      protocolPackageName: Option[String],
      generateMutable: Boolean,
      generateVisitor: Boolean,
      laxAny: Boolean,
      additionalCombinedPackageNames: Map[Option[String], Option[String]],
      generateClients: List[HttpClient],
      ignoreUnknown: Boolean,
      vararg: Boolean
    ) {

      def apply = {
        def compile: Seq[File] =
          sources.headOption map { src =>
            import sc._
            sc.Log.configureLogger(verbose)
            val module = Module.moduleByFileName(src)
            module.processFiles(sources.toVector, scConfig.update(Outdir(outputDirectory)))
          } getOrElse {Nil}

        compile
      }

      def combinedPackageNames: Map[Option[String], Option[String]] =
        generatedPackageNames.map { case (k, v) => ((Option(k.toString), Some(v))) }.updated(None, Some(generatedPackageName))

      def scConfig: ScConfig =
        ScConfig(
          Vector(PackageNames(combinedPackageNames)) ++
          (if (packageDir) Vector(GeneratePackageDir) else Vector()) ++
          classPrefix.map(ClassPrefix(_)).toVector ++
          paramPrefix.map(ParamPrefix(_)).toVector ++
          attributePrefix.map(AttributePrefix(_)).toVector ++
          Vector(ScConfig.defaultOutdir) ++
          (if (prependFamily.value) Vector(PrependFamilyName) else Vector()) ++
          Vector(WrappedComplexTypes(wrapContents)) ++
          Vector(SeperateProtocol) ++
          Vector(ProtocolFileName(protocolFileName)) ++
          Vector(ProtocolPackageName(protocolPackageName)) ++
          Vector(ScConfig.defaultDefaultNamespace) ++
          (if (generateRuntime) Vector(GenerateRuntime) else Vector()) ++
          (generateClients.toVector.flatMap {
            case Dispatch(version, true) => Vector(GenerateDispatchClient, DispatchVersion(version), GenerateDispatchAs)
            case Dispatch(version, false) => Vector(GenerateDispatchClient, DispatchVersion(version))
            case Gigahorse(version, Gigahorse.OkHttp) => Vector(GenerateGigahorseClient, GigahorseVersion(version), GigahorseBackend("okhttp"))
            case Gigahorse(version, Gigahorse.AHC) => Vector(GenerateGigahorseClient, GigahorseVersion(version), GigahorseBackend("asynchttpclient"))
            case Async => Vector(GenerateAsync)
          }) ++
          Vector(ContentsSizeLimit(contentSizeLimit)) ++
          Vector(SequenceChunkSize(chunkSize)) ++
          (if (namedAttributes) Vector(NamedAttributes) else Vector()) ++
          (if (laxAny) Vector(LaxAny) else Vector()) ++
          (if (ignoreUnknown) Vector(IgnoreUnknown) else Vector()) ++
          (if (vararg && !generateMutable) Vector(VarArg) else Vector()) ++
          (if (generateMutable) Vector(GenerateMutable) else Vector()) ++
          (if (generateVisitor) Vector(GenerateVisitor) else Vector()) ++
          (if (autoPackages) Vector(AutoPackages) else Vector())
        )

    }
  }


  object ScalaXBConfig {
  }

  sealed trait HttpClient
  case class Dispatch(version: String, generateAs: Boolean = false) extends HttpClient
  case class Gigahorse(version: String, backend: Gigahorse.Backend) extends HttpClient
  case object Async extends HttpClient
  object Gigahorse {
    sealed trait Backend
    case object OkHttp extends Backend
    case object AHC extends Backend
  }
}

