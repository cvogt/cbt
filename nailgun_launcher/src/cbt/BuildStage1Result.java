package cbt;
public class BuildStage1Result{
  public long start;
  public long stage1LastModified;
  public ClassLoader classLoader;
  public String stage1Classpath;
  public String nailgunClasspath;
  public String compatibilityClasspath;
  public BuildStage1Result( long start, long stage1LastModified, ClassLoader classLoader, String stage1Classpath, String nailgunClasspath, String compatibilityClasspath ){
    this.start = start;
    this.stage1LastModified = stage1LastModified;
    this.classLoader = classLoader;
    this.stage1Classpath = stage1Classpath;
    this.nailgunClasspath = nailgunClasspath;
    this.compatibilityClasspath = compatibilityClasspath;
  }
}
