package cbt;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

// TODO: try to reduce the number of members
public abstract class Context{
  public abstract File projectDirectory();
  public abstract File cwd();
  public abstract String[] argsArray();
  public abstract String[] enabledLoggersArray();
  public abstract Long startCompat();
  public abstract Boolean cbtHasChangedCompat();
  public abstract String versionOrNull();
  public abstract String scalaVersionOrNull(); // needed to propagate scalaVersion to dependendee builds
  public abstract ConcurrentHashMap<String,Object> permanentKeys();
  public abstract ConcurrentHashMap<Object,ClassLoader> permanentClassLoaders();
  public abstract File cache();
  public abstract File cbtHome();
  public abstract File cbtRootHome();
  public abstract File compatibilityTarget();
  public abstract BuildInterface parentBuildOrNull();
}
