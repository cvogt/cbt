package cbt

import cbt._
import scala.xml._
import java.io._

trait ExportBuildInformation {self: BaseBuild =>
  lazy val printer = new scala.xml.PrettyPrinter(200, 2)

  def buildInfoXml = 
    printer.format(BuildInformationSerializer.serialize(BuildInformation.Project(self)))
}

object BuildInformation {
  case class Project(name: String,
             root: File,
             rootModule: Module,
             modules: Seq[Module],
             libraries: Seq[Library]) 

  case class Module(name: String,
            root: File,
            sourcesRoot: File,
            sources: Seq[File],
            mavenDependencies: Seq[MavenDependency],
            moduleDependencies: Seq[ModuleDependency],
            targetLibraries: Seq[TargetLibrary],
            parentBuild: Option[String])

  case class Library(name: String) 

  case class MavenDependency(name: String)

  case class ModuleDependency(name: String)

  case class TargetLibrary(path: File)

  object Project {
    def apply(build: BaseBuild) =   
     new BuildInformationExporter(build).exportBuildInformation  

    private class BuildInformationExporter(rootBuild: BaseBuild) {
      def exportBuildInformation(): Project = {
        val rootModule = exportModule(rootBuild)
        val modules = (rootBuild +: rootBuild.transitiveDependencies)
          .collect { case d: BaseBuild => d +: collectParentBuilds(d)}
          .flatten
          .map(exportModule)
          .distinct
        val libraries = rootBuild.transitiveDependencies
          .collect { case d: BoundMavenDependency => Library(fomatMavenDependency(d.mavenDependency))}
          .distinct
       
        Project(rootModule.name,
            rootModule.root,
            rootModule,
            modules,
            libraries)
      }

      private def collectParentBuilds(build: BaseBuild): Seq[BaseBuild] = 
          build.context.parentBuild
            .map(_.asInstanceOf[BaseBuild])
            .map(b => b +: collectParentBuilds(b))
            .toSeq
            .flatten

      private def exportModule(build: BaseBuild): Module = {
        val moduleDependencies = build.dependencies
          .collect { case d: BaseBuild => ModuleDependency(moduleName(d)) }
        val mavenDependencies = build.dependencies
          .collect { case d: BoundMavenDependency => MavenDependency(fomatMavenDependency(d.mavenDependency))}
        val targetLibraries = build.dependencyClasspath.files
          .filter(_.isFile)
          .map(t => TargetLibrary(t))
        Module(moduleName(build),
            build.projectDirectory,
            build.projectDirectory,
            build.sources.filter(_.exists),
            mavenDependencies,
            moduleDependencies,
            targetLibraries,
            build.context.parentBuild.map(b => moduleName(b.asInstanceOf[BaseBuild])))    
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
      <root>{module.root.toString}</root>
      <sourcesRoot>{module.sourcesRoot.toString}</sourcesRoot>      
      <sources>
        {module.sources.map(s => <source>{s.string}</source>)}
      </sources>
      <mavenDependencies>
        {module.mavenDependencies.map(serialize)}
      </mavenDependencies>
      <moduleDependencies>
        {module.moduleDependencies.map(serialize)}
      </moduleDependencies>
      <targetLibraries>
        {module.targetLibraries.map(serialize)}
      </targetLibraries>
      {module.parentBuild.map(p => <parentBuild>{p}</parentBuild>).getOrElse(NodeSeq.Empty)}
    </module>

  private def serialize(mavenDependency: BuildInformation.MavenDependency): Node = 
    <mavenDependency>{mavenDependency.name}</mavenDependency>

  private def serialize(library: BuildInformation.Library): Node = 
    <library>{library.name}</library>

  private def serialize(moduleDependency: BuildInformation.ModuleDependency): Node = 
    <moduleDependency>{moduleDependency.name}</moduleDependency>

  private def serialize(targetLibrary: BuildInformation.TargetLibrary): Node = 
    <targetLibrary>{targetLibrary.path.toString}</targetLibrary>
}
