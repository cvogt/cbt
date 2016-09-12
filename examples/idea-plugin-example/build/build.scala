
import cbt._
import idea_plugin.IdeaPlugin


class Build(val context: Context) extends BaseBuild with IdeaPlugin {
  override def projectName = "idea-plugin-example"
}
