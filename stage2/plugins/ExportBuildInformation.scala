package cbt

import cbt._
import scala.xml._
import java.io._

trait ExportBuildInformation { self: BaseBuild =>
  lazy val printer = new scala.xml.PrettyPrinter(200, 2)

  def buildInfoXml = 
    printer.format(BuildInformationSerializer.serialize(BuildInformation.Project(self)))

  def a = DirectoryDependency(context.cbtHome).dependenciesArray.map(_.toString).mkString("\n")
}

object BuildInformation {
  case class Project(
    name: String,
    root: File,
    rootModule: Module,
    modules: Seq[Module],
    libraries: Seq[Library]
  )

  case class Module(
    name: String,
    root: File,
    scalaVersion: String,
    sources: Seq[File],
    target: File,
    libraryDependencies: Seq[LibraryDependency],
    moduleDependencies: Seq[ModuleDependency],
    classpaths: Seq[ClassPathItem],
    parentBuild: Option[String]
  )

  case class Library( name: String, jars: Seq[File] )

  case class LibraryDependency( name: String )

  case class ModuleDependency( name: String )

  case class ClassPathItem( path: File )

  object Project {
    def apply(build: BaseBuild) =   
     new BuildInformationExporter(build).exportBuildInformation  

    private class BuildInformationExporter(rootBuild: BaseBuild) {
      def exportBuildInformation(): Project = {
        val moduleBuilds = collectBuildls(rootBuild)
        val libraries = moduleBuilds
          .flatMap(_.transitiveDependencies)
          .collect { case d: BoundMavenDependency => exportLibrary(d)}
          .distinct
        val cbtLibraries = 
          collectBuildls(DirectoryDependency(rootBuild.context.cbtHome)(rootBuild.context).dependenciesArray.head.asInstanceOf[BaseBuild])
            .collect {
              case d: BoundMavenDependency => d.jar
              case d: PackageJars => d.jar.get
            }
          .map(exportLibrary)
          .distinct
        val cbtLibraryDependencies = cbtLibraries
          .map(l => LibraryDependency(l.name))
        val rootModule = exportModule(cbtLibraryDependencies)(rootBuild)
        val modules = moduleBuilds.map(exportModule(cbtLibraryDependencies))
        Project(
          rootModule.name,
          rootModule.root,
          rootModule,
          modules,
          libraries ++ cbtLibraries
        )
      }

      def collectBuildls(build: BaseBuild) = 
        (build +: build.transitiveDependencies)
          .collect { case d: BaseBuild => d +: collectParentBuilds(d)}
          .flatten
          .distinct

      private def exportLibrary(mavenDependency: BoundMavenDependency) = 
        Library(fomatMavenDependency(mavenDependency.mavenDependency), mavenDependency.exportedJars)

      private def exportLibrary(file: File) = 
        Library("CBT:" + file.getName.stripSuffix(".jar"), Seq(file))

      private def collectParentBuilds(build: BaseBuild): Seq[BaseBuild] = 
          build.context.parentBuild
            .map(_.asInstanceOf[BaseBuild])
            .map(b => b +: collectParentBuilds(b))
            .toSeq
            .flatten

      private def exportModule(cbtLibraryDependencies: Seq[LibraryDependency])(build: BaseBuild): Module = {       
        val moduleDependencies = build.dependencies
          .collect { case d: BaseBuild => ModuleDependency(moduleName(d)) }
        val mavenDependencies = build.dependencies
          .collect { case d: BoundMavenDependency => LibraryDependency(fomatMavenDependency(d.mavenDependency))}
        val classpaths = build.dependencyClasspath.files
          .filter(_.isFile)
          .map(t => ClassPathItem(t))
        val sources = {
          val s = build.sources
            .filter(s => s.exists && s.isDirectory)
          if (s.isEmpty)
            Seq(build.projectDirectory)
          else
            s
        }
        Module(
          name = moduleName(build),
          root = build.projectDirectory,
          scalaVersion = build.scalaVersion,
          sources = sources,
          target = build.target,
          libraryDependencies = mavenDependencies ++ cbtLibraryDependencies,
          moduleDependencies = moduleDependencies,
          classpaths = classpaths,
          parentBuild = build.context.parentBuild.map(b => moduleName(b.asInstanceOf[BaseBuild]))
        )
      }

      private def fomatMavenDependency(dependency: cbt.MavenDependency) =
        s"${dependency.groupId}:${dependency.artifactId}:${dependency.version}"

      private def moduleName(build: BaseBuild) = 
          if (rootBuild.projectDirectory == build.projectDirectory)
            rootBuild.projectDirectory.getName
          else
            build.projectDirectory.getPath
              .drop(rootBuild.projectDirectory.getPath.length)
              .stripPrefix("/")
              .replace("/", "-")
    }
  }
}

object BuildInformationSerializer {
  def serialize(project: BuildInformation.Project): Node = 
    <project>
      <name>{project.name}</name>
      <root>{project.root.toString}</root>
      <rootModule>{project.rootModule.name}</rootModule>
      <modules>
        {project.modules.map(serialize)}
      </modules>
      <libraries>
        {project.libraries.map(serialize)}
      </libraries>
    </project>

  private def serialize(module: BuildInformation.Module): Node = 
    <module>
      <name>{module.name}</name>
      <root>{module.root}</root>
      <target>{module.target}</target>      
      <scalaVersion>{module.scalaVersion}</scalaVersion>
      <sources>
        {module.sources.map(s => <source>{s}</source>)}
      </sources>
      <libraryDependencies>
        {module.libraryDependencies.map(serialize)}
      </libraryDependencies>
      <moduleDependencies>
        {module.moduleDependencies.map(serialize)}
      </moduleDependencies>
      <classpaths>
        {module.classpaths.map(serialize)}
      </classpaths>
      {module.parentBuild.map(p => <parentBuild>{p}</parentBuild>).getOrElse(NodeSeq.Empty)}
    </module>

  private def serialize(libraryDependency: BuildInformation.LibraryDependency): Node = 
    <libraryDependency>{libraryDependency.name}</libraryDependency>

  private def serialize(library: BuildInformation.Library): Node = 
    <library>
      <name>{library.name}</name>
      <jars>
        {library.jars.map(j => <jar>{j}</jar>)}
      </jars>
    </library>

  private def serialize(moduleDependency: BuildInformation.ModuleDependency): Node = 
    <moduleDependency>{moduleDependency.name}</moduleDependency>

  private def serialize(targetLibrary: BuildInformation.ClassPathItem): Node = 
    <classpathItem>{targetLibrary.path.toString}</classpathItem>
}
