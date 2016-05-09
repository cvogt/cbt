package cbt;

public interface ArtifactInfo extends Dependency{
  public abstract String artifactId();
  public abstract String groupId();
  public abstract String version();
}
