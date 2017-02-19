package cbt;
import java.io.*;

public interface BuildInterface extends Dependency{
  // needed to propagage through build builds. Maybe we can get rid of this.
  public default BuildInterface finalBuild(File current){
    return finalBuild(); // legacy forwarder
  }
  public abstract File[] triggerLoopFilesArray(); // needed for watching files across composed builds

  // deprecated methods, which clients are still allowed to implement, but not required
  public abstract BuildInterface finalBuild(); // needed to propagage through build builds. Maybe we can get rid of this.
  public abstract BuildInterface copy(Context context);
  public abstract String scalaVersion();
  public abstract String[] crossScalaVersionsArray();
}
