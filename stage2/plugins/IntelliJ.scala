package cbt

import java.io.{ File, FileWriter }

trait IntelliJ extends BaseBuild {
  private // this plugin is not functional right now, needs fixing
  def intellij = {
    lib.writeIfChanged(
      projectDirectory / name ++ ".iml",
      IntelliJ.iml(
        scalaVersion,
        transitiveDependencies
      )
    )
  }
}

// TODO:
// - projects, their builds, their build builds, etc should be represented individually
// ideally with cbt's main sources being editable, but not the sources of other cbt versions
object IntelliJ {
  def iml(
    scalaVersion:           String,
    transitiveDependencies: Seq[Dependency]
  ): String = {
    """<?xml version="1.0" encoding="UTF-8"?>""" ++ "\n" ++
// format: OFF
<module type="JAVA_MODULE" version="4">
  <component name="NewModuleRootManager" inherit-compiler-output="true">
    <exclude-output/>
    <content url="file://$MODULE_DIR$/../cbt/compatibility">
      <sourceFolder url="file://$MODULE_DIR$/../cbt/compatibility" isTestSource="false"/>
      <excludeFolder url="file://$MODULE_DIR$/../cbt/compatibility/target/scala-2.11"/>
    </content>
    <content url="file://$MODULE_DIR$/../cbt/nailgun_launcher">
      <sourceFolder url="file://$MODULE_DIR$/../cbt/nailgun_launcher" isTestSource="false"/>
      <excludeFolder url="file://$MODULE_DIR$/../cbt/nailgun_launcher/target"/>
    </content>
    <content url="file://$MODULE_DIR$/../cbt/stage1">
      <sourceFolder url="file://$MODULE_DIR$/../cbt/stage1" isTestSource="false"/>
      <excludeFolder url="file://$MODULE_DIR$/../cbt/stage1/target/scala-2.11"/>
    </content>
    <content url="file://$MODULE_DIR$/../cbt/stage2">
      <sourceFolder url="file://$MODULE_DIR$/../cbt/stage2" isTestSource="false"/>
      <excludeFolder url="file://$MODULE_DIR$/../cbt/stage2/target/scala-2.11"/>
    </content>
    <content url="file://$MODULE_DIR$">
      <sourceFolder url="file://$MODULE_DIR$/src/main/scala/main" isTestSource="false"/>
      <sourceFolder url="file://$MODULE_DIR$/src/test/scala" isTestSource="false"/>
    </content>
    <orderEntry type="inheritedJdk"/>
    <orderEntry type="sourceFolder" forTests="false"/>
    <orderEntry type="library" name="scala-sdk-{scalaVersion}" level="application"/>
    {
      transitiveDependencies.flatMap( _.exportedClasspath.files ).map( _.string ).map { cp =>
        <orderEntry type="module-library">
          <library>
            <CLASSES>
              <root url={ if ( cp.endsWith( ".jar" ) ) s"jar://$cp!/" else s"file://$cp/" }/>
            </CLASSES>
            <JAVADOC/>
            <SOURCES/>
          </library>
        </orderEntry>
      }
    }
  </component>
</module>.buildString( stripComments = false ) // format: ON
  }
}
