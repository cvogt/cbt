package cbt;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

// TODO: try to reduce the number of members
public abstract class Context{
  public abstract File projectDirectory();
  public abstract File cwd(); // REPLACE by something that allows to run cbt on some other directly
  public abstract String[] argsArray(); // replace this by https://github.com/cvogt/cbt/issues/172 ?
  public abstract String[] enabledLoggersArray();
  public abstract Long startCompat();
  public abstract Boolean cbtHasChangedCompat();
  public abstract String scalaVersionOrNull(); // needed to propagate scalaVersion to dependendee builds
  public abstract ConcurrentHashMap<Object,Object> persistentCache();
  public abstract ConcurrentHashMap<Object,Object> transientCache();
  public abstract File cache();
  public abstract File cbtHome();
  public abstract File cbtRootHome(); // REMOVE
  public abstract File compatibilityTarget(); // maybe replace this with search in the classloader for it?
  public abstract BuildInterface parentBuildOrNull();
}
