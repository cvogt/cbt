
import cbt._
import idea_plugin.IdeaPlugin


class Build(val context: Context) extends BaseBuild with IdeaPlugin {
  override def name = "idea-plugin-example"
}
