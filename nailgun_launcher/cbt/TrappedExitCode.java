package cbt;
import java.security.*;
public class TrappedExitCode extends SecurityException{
  public int exitCode;
  public TrappedExitCode(int exitCode){
    this.exitCode = exitCode;
  }
}
