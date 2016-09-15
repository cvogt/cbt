package cbt;
import java.security.*;
/*
When enabled, this SecurityManager turns System.exit(...) calls into exceptions that can be caught and handled.
Installing a SecurityManager is a global side-effect and thus needs extra care in a persistent
background process like CBT's. The current approach is install it once during JVM-startup.
When disabled this delegates to the SecurityManager installed before if any, which
would be Nailgun's if running on Nailgun. If we do not delegate to Nailgun, it seems we
could in some cases kill the server process
*/
public class TrapSecurityManager extends ProxySecurityManager{
  public TrapSecurityManager(){
    super(NailgunLauncher.initialSecurityManager);
  }
  
  public void checkPermission( Permission permission ){
    /*
    NOTE: is it actually ok, to just make these empty?
    Calling .super leads to ClassNotFound exteption for a lambda.
    Calling to the previous SecurityManager leads to a stack overflow
    */
    if(!NailgunLauncher.trapExitCode.get()){
      super.checkPermission(permission);
    }
  }
  public void checkPermission( Permission permission, Object context ){
    /* Does this methods need to be overidden? */
    if(!NailgunLauncher.trapExitCode.get()){
      super.checkPermission(permission, context);
    }
  }
  @Override
  public void checkExit( int status ){
    if(NailgunLauncher.trapExitCode.get()){
      throw new TrappedExitCode(status);
    }
    super.checkExit(status);
  }
}
