package cbt;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

// TODO: try to reduce the number of members
public interface Context{
  // recently added methods that needs default values for old versions to work
  public default long cbtLastModified(){
    throw new IncompatibleCbtVersionException("You need to define method cbtLastModified.");
  };
  public default Map<Object,Object> persistentCache(){
    throw new IncompatibleCbtVersionException("You need to define method persistentCache.");
  };
  public default Map<Object,Object> transientCache(){
    throw new IncompatibleCbtVersionException("You need to define method transientCache.");
  };
  public default long start(){
    throw new IncompatibleCbtVersionException("You need to define method start.");
  };
  public default File workingDirectory(){
    return projectDirectory();
  };

  // methods that exist for longer which every CBT version in use should have by now, no default values needed
  public abstract File cwd(); // REPLACE by something that allows to run cbt on some other directly
  public abstract String[] argsArray(); // replace this by https://github.com/cvogt/cbt/issues/172 ?
  public abstract String[] enabledLoggersArray();
  public abstract String scalaVersionOrNull(); // needed to propagate scalaVersion to dependendee builds
  public abstract File cache();
  public abstract File cbtHome();
  public abstract File cbtRootHome(); // REMOVE
  public abstract File compatibilityTarget(); // maybe replace this with search in the classloader for it?
  public abstract BuildInterface parentBuildOrNull();

  // deprecated methods
  @java.lang.Deprecated
  public abstract Long startCompat();
  @java.lang.Deprecated
  public abstract Boolean cbtHasChangedCompat();
  @java.lang.Deprecated
  public abstract ConcurrentHashMap<String,Object> permanentKeys();
  @java.lang.Deprecated
  public abstract ConcurrentHashMap<Object,ClassLoader> permanentClassLoaders();
  @java.lang.Deprecated
  public abstract File projectDirectory();
}
