package cbt
import java.nio.file._
import java.io.File

class ConcreteBuildBuild(val context: Context) extends BuildBuild

trait BuildBuild extends BaseBuild{
  override def dependencies = super.dependencies :+ context.cbtDependency

  object plugins extends plugins( context, scalaVersion )

  /** CBT relies on hierarchical classloaders */
  final override def flatClassLoader = false

  assert(
    projectDirectory.getName === lib.buildDirectoryName,
    s"You can't extend ${lib.buildBuildClassName} in: " + projectDirectory + "/" + lib.buildDirectoryName
  )
}

trait CbtInternal extends BaseBuild{
  protected object cbtInternal{
    def shared = DirectoryDependency(context.cbtHome / "/internal/plugins/shared")
    def library = DirectoryDependency(context.cbtHome / "/internal/plugins/library")
  }
}
