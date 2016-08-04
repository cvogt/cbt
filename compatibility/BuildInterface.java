package cbt;
import java.nio.*;
import java.nio.file.*;

public interface BuildInterface extends Dependency{
  public abstract BuildInterface copy(Context context);  // needed to configure builds
  public abstract String scalaVersion(); // needed to propagate scalaVersion to dependent builds
  public abstract String[] crossScalaVersionsArray(); // FIXME: this probably can't use Scala classes
  public abstract BuildInterface finalBuild(); // needed to propagage through build builds. Maybe we can get rid of this.
  public abstract Path[] triggerLoopFilesArray(); // needed for watching files across composed builds
}
