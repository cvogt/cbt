package cbt;
import java.nio.file.*;
import java.util.concurrent.ConcurrentHashMap;

// TODO: try to reduce the number of members
public abstract class Context{
  public abstract Path projectDirectory();
  public abstract Path cwd();
  public abstract String[] argsArray();
  public abstract String[] enabledLoggersArray();
  public abstract Long startCompat();
  public abstract Boolean cbtHasChangedCompat();
  public abstract String versionOrNull();
  public abstract String scalaVersionOrNull(); // needed to propagate scalaVersion to dependendee builds
  public abstract ConcurrentHashMap<String,Object> permanentKeys();
  public abstract ConcurrentHashMap<Object,ClassLoader> permanentClassLoaders();
  public abstract Path cache();
  public abstract Path cbtHome();
  public abstract Path cbtRootHome();
  public abstract Path compatibilityTarget();
  public abstract BuildInterface parentBuildOrNull();
}
