package cbt

import cbt._
import scala.xml._
import scala.util._
import java.io._
import scala.collection.JavaConverters._

trait ExportBuildInformation { self: BaseBuild =>
  lazy val printer = new scala.xml.PrettyPrinter(200, 2)

  def buildInfoXml = 
    printer.format(BuildInformationSerializer.serialize(BuildInformation.Project(self)))
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

    class BuildInformationExporter(rootBuild: BaseBuild) {
      def exportBuildInformation(): Project = {
        val moduleBuilds = transitiveBuilds(rootBuild)
        val libraries = moduleBuilds
          .flatMap(_.transitiveDependencies)
          .collect { case d: BoundMavenDependency => exportLibrary(d)}
          .distinct
        val cbtLibraries = convertCbtLibraries
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

      private def convertCbtLibraries() = 
        transitiveBuilds(DirectoryDependency(rootBuild.context.cbtHome)(rootBuild.context).dependenciesArray.head.asInstanceOf[BaseBuild])
        .collect {
          case d: BoundMavenDependency => d.jar
          case d: PackageJars => d.jar.get
        }
        .map(exportLibrary)
        .distinct

          
      private def collectLazyBuilds(dependency: Dependency): Option[BaseBuild] = 
        dependency match {
          case l: LazyDependency =>         
            l.dependency match {
               case d: BaseBuild => Some(d)
               case d: LazyDependency => collectLazyBuilds(d.dependency)
               case _ => None
            }
          case d: BaseBuild => Some(d)
          case _ => None
        }


      private def transitiveBuilds(build: BaseBuild): Seq[BaseBuild] =             
        (build +: build.transitiveDependencies)
        .collect {
          case d: BaseBuild => d +: collectParentBuilds(d).flatMap(transitiveBuilds)
          case d: LazyDependency => 
            collectLazyBuilds(d.dependency)
            .toSeq
            .flatMap(transitiveBuilds)
        }
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
        def collectDependencies(dependencies: Seq[Dependency]): Seq[ModuleDependency] = 
          dependencies
          .collect {
             case d: BaseBuild => Seq(ModuleDependency(moduleName(d)))
             case d: LazyDependency => collectDependencies(Seq(d.dependency))
           }
          .flatten      
        val moduleDependencies = collectDependencies(build.dependencies)
        val mavenDependencies = build.dependencies
          .collect { case d: BoundMavenDependency => LibraryDependency(fomatMavenDependency(d.mavenDependency))}
        val classpaths = build.dependencyClasspath.files
          .filter(_.isFile)
          .map(t => ClassPathItem(t))
        val sources = build.sources
          .filter(s => s.exists && s.isDirectory) :+ build.projectDirectory       
        
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
    <project name={project.name} root={project.root.toString} rootModule={project.rootModule.name}> 
      <modules>
        {project.modules.map(serialize)}
      </modules>
      <libraries>
        {project.libraries.map(serialize)}
      </libraries>
    </project>

  private def serialize(module: BuildInformation.Module): Node = 
    <module name={module.name} root={module.root.toString} target={module.target.toString} scalaVersion={module.scalaVersion}>
      <sources>
        {module.sources.map(s => <source>{s}</source>)}
      </sources>
      <dependencies>
        {module.libraryDependencies.map(serialize)}     
        {module.moduleDependencies.map(serialize)}
      </dependencies>
      <classpath>
        {module.classpaths.map(serialize)}
      </classpath>
      {module.parentBuild.map(p => <parentBuild>{p}</parentBuild>).getOrElse(NodeSeq.Empty)}
    </module>

  private def serialize(libraryDependency: BuildInformation.LibraryDependency): Node = 
    <libraryDependency>{libraryDependency.name}</libraryDependency>

  private def serialize(library: BuildInformation.Library): Node = 
    <library name = {library.name}>
      {library.jars.map(j => <jar>{j}</jar>)}
    </library>

  private def serialize(moduleDependency: BuildInformation.ModuleDependency): Node = 
    <moduleDependency>{moduleDependency.name}</moduleDependency>

  private def serialize(targetLibrary: BuildInformation.ClassPathItem): Node = 
    <classpathItem>{targetLibrary.path.toString}</classpathItem>
}
