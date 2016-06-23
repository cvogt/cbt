package idea_plugin

import java.io.{File, FileWriter}

import cbt.{BaseBuild, ExitCode}


trait IdeaPlugin extends BaseBuild {

  import IdeaPlugin._

  // @TODO add only the top level dependencies
  def generateIdeaProject: ExitCode = {
    val moduleDir = "$MODULE_DIR$"

    val projectDependencies: List[String] = for {
      depJarFile <- this.dependencies
        .flatMap(dep => dep.dependenciesArray().toList)
        .flatMap(x => x.exportedClasspathArray().toList).toList
      depIdeaOrderEntry =
      s"""<orderEntry type="module-library">
          |      <library>
          |        <CLASSES>
          |            <root url="jar://$moduleDir/../cbt/${depJarFile.getPath.split("/cbt")(1)}!/" />
          |          </CLASSES>
          |          <JAVADOC />
          |          <SOURCES />
          |       </library>
          |</orderEntry>""".stripMargin
    } yield depIdeaOrderEntry

    val imlFile = new File(projectDirectory.getPath + s"/$projectName.iml")
    if (!imlFile.exists()) {
      imlFile.createNewFile()
    }
    val fw = new FileWriter(imlFile.getPath, false)
    fw.write(templateWithCBTSources(projectDependencies.mkString("\n")))
    fw.close()
    ExitCode.Success
  }

}

private[idea_plugin] object IdeaPlugin {

  // @TODO inject from the build.scala
  // scala version
  // path of cbt relative to module dir

  private val templateWithCBTSources: (String) => String = (dependencies: String) =>
    """<?xml version="1.0" encoding="UTF-8"?>
      |<module type="JAVA_MODULE" version="4">
      |  <component name="NewModuleRootManager" inherit-compiler-output="true">
      |    <exclude-output />
      |    <content url="file://$MODULE_DIR$/../cbt/compatibility">
      |      <sourceFolder url="file://$MODULE_DIR$/../cbt/compatibility" isTestSource="false" />
      |      <excludeFolder url="file://$MODULE_DIR$/../cbt/compatibility/target/scala-2.11" />
      |    </content>
      |    <content url="file://$MODULE_DIR$/../cbt/nailgun_launcher">
      |      <sourceFolder url="file://$MODULE_DIR$/../cbt/nailgun_launcher" isTestSource="false" />
      |      <excludeFolder url="file://$MODULE_DIR$/../cbt/nailgun_launcher/target" />
      |    </content>
      |    <content url="file://$MODULE_DIR$/../cbt/stage1">
      |      <sourceFolder url="file://$MODULE_DIR$/../cbt/stage1" isTestSource="false" />
      |      <excludeFolder url="file://$MODULE_DIR$/../cbt/stage1/target/scala-2.11" />
      |    </content>
      |    <content url="file://$MODULE_DIR$/../cbt/stage2">
      |      <sourceFolder url="file://$MODULE_DIR$/../cbt/stage2" isTestSource="false" />
      |      <excludeFolder url="file://$MODULE_DIR$/../cbt/stage2/target/scala-2.11" />
      |    </content>
      |    <content url="file://$MODULE_DIR$">
      |      <sourceFolder url="file://$MODULE_DIR$/src/main/scala/main" isTestSource="false" />
      |      <sourceFolder url="file://$MODULE_DIR$/src/test/scala" isTestSource="false" />
      |    </content>
      |    <orderEntry type="inheritedJdk" />
      |    <orderEntry type="sourceFolder" forTests="false" />
      |    <orderEntry type="library" name="scala-sdk-2.11.7" level="application" /> |
      |    """.stripMargin + dependencies +
      """  </component>
        |</module>""".stripMargin
}
