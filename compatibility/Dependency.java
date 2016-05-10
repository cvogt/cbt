package cbt;
import java.io.*;

public interface Dependency{
  public abstract String show();
  public abstract Boolean needsUpdateCompat();
  public abstract Dependency[] dependenciesArray();
  public abstract File[] dependencyClasspathArray();
  public abstract File[] exportedClasspathArray();
}
