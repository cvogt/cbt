package cbt;
import java.io.*;

public abstract class BuildInterface implements Dependency{
  public abstract BuildInterface copy(Context context);  // needed to configure builds
  public abstract String scalaVersion(); // needed to propagate scalaVersion to dependent builds
  public abstract BuildInterface finalBuild(); // needed to propagage through build builds. Maybe we can get rid of this.
  public abstract File[] triggerLoopFilesArray(); // needed for watching files across composed builds

  // deprecated methods, which clients are still allowed to implement, but not required
  public abstract String[] crossScalaVersionsArray();
}
