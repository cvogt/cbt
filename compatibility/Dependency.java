package cbt;
import java.nio.*;
import java.nio.file.*;

public interface Dependency{
  public abstract String show();
  public abstract Boolean needsUpdateCompat();
  public abstract Dependency[] dependenciesArray();
  public abstract Path[] dependencyClasspathArray();
  public abstract Path[] exportedClasspathArray();
}
