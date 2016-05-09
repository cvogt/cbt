package cbt;
import java.security.*;
public class TrapSecurityManager extends SecurityManager{
  public void checkPermission( Permission permission ){
    /*
    NOTE: is it actually ok, to just make these empty?
    Calling .super leads to ClassNotFound exteption for a lambda.
    Calling to the previous SecurityManager leads to a stack overflow
    */
  }
  public void checkPermission( Permission permission, Object context ){
    /* Does this methods need to be overidden? */
  }
  @Override
  public void checkExit( int status ){
    super.checkExit(status);
    throw new TrappedExitCode(status);
  }
}
