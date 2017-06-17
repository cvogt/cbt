package cbt;
import java.io.*;

public interface Dependency{
  // recently added methods that needs default values for old versions to work
  public default String moduleKey(){
    throw new IncompatibleCbtVersionException("You need to override this method.");
  };
  public default long lastModified(){
    throw new IncompatibleCbtVersionException("You need to override this method.");
  };
  public default ClassLoader classLoader(){
    throw new IncompatibleCbtVersionException("You need to override this method.");
  };

  // methods that exist for longer which every CBT version in use should have by now, no default values needed
  public abstract String show();
  public abstract Dependency[] dependenciesArray();
  public abstract File[] exportedClasspathArray();

  // deprecated methods
  @java.lang.Deprecated
  public abstract boolean needsUpdateCompat();
  @java.lang.Deprecated
  public abstract File[] dependencyClasspathArray();
}

