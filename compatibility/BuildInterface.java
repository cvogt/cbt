package cbt;
import java.io.*;

public interface BuildInterface extends Dependency{
  // needed to propagage through build builds. Maybe we can get rid of this.
  public default BuildInterface finalBuild(File current){
    return finalBuild(); // legacy forwarder
  }
  @Deprecated
  public default File[] triggerLoopFilesArray(){
    return new File[0];
  };

  // deprecated methods, which clients are still allowed to implement, but not required
  public abstract BuildInterface finalBuild(); // needed to propagage through build builds. Maybe we can get rid of this.
  public abstract BuildInterface copy(Context context);
  public abstract String scalaVersion();
  public abstract String[] crossScalaVersionsArray();
}
