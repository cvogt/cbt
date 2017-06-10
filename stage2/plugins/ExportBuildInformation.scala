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
             modules: Seq[Module]) 

  case class Module(name: String,
            root: File,
            sourcesRoot: File,
            sources: Seq[File],
            mavenDependencies: Seq[MavenDependency],
            moduleDependencies: Seq[ModuleDependency],
            parentBuild: Option[String])

  case class MavenDependency(groupId: String,
            artifactId: String,
            version: String)

  case class ModuleDependency(name: String)

  object Project {
    def apply(build: BaseBuild) =   
     new BuildInformationExporter(build).exportBuildInformation  

    private class BuildInformationExporter(rootBuild: BaseBuild) {
      def exportBuildInformation(): Project = {
        val rootModule = exportModule(rootBuild)
        val otherModules = rootBuild.transitiveDependencies
          .collect { case d: BaseBuild => d +: collectParentBuilds(d)}
          .flatten
          .map(exportModule)
          .distinct
   
        Project(rootModule.name,
            rootModule.root,
            rootModule,
            rootModule +: otherModules)
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
          .collect { case d: BoundMavenDependency => MavenDependency(d.groupId, d.artifactId, d.version)}

        Module(moduleName(build),
            build.projectDirectory,
            build.projectDirectory,
            build.sources.filter(_.exists),
            mavenDependencies,
            moduleDependencies,
            build.context.parentBuild.map(b => moduleName(b.asInstanceOf[BaseBuild])))    
      }

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
      {module.parentBuild.map(p => <parentBuild>{p}</parentBuild>).getOrElse(NodeSeq.Empty)}
    </module>

  private def serialize(mavenDependency: BuildInformation.MavenDependency): Node = 
    <mavenDependency>
      {s"${mavenDependency.groupId}:${mavenDependency.artifactId}:${mavenDependency.version}"}
    </mavenDependency>

  private def serialize(moduleDependency: BuildInformation.ModuleDependency): Node = 
    <moduleDependency>{moduleDependency.name}</moduleDependency>
}
