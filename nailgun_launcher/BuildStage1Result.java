package cbt;
public class BuildStage1Result{
  public Boolean changed;
  public ClassLoader classLoader;
  public String stage1Classpath;
  public String nailgunClasspath;
  public String compatibilityClasspath;
  public BuildStage1Result( Boolean changed, ClassLoader classLoader, String stage1Classpath, String nailgunClasspath, String compatibilityClasspath ){
    this.changed = changed;
    this.classLoader = classLoader;
    this.stage1Classpath = stage1Classpath;
    this.nailgunClasspath = nailgunClasspath;
    this.compatibilityClasspath = compatibilityClasspath;
  }
}
