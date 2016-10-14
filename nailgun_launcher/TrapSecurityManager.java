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
  public static ThreadLocal<Boolean> trapExitCode(){
    // storing the flag in the installed security manager
    // instead of e.g. a static member is necessary because
    // we run multiple versions of CBT with multiple TrapSecurityManager classes
    // but we need to affect the installed one
    SecurityManager sm = System.getSecurityManager();
    if(sm instanceof TrapSecurityManager){
      return ((TrapSecurityManager) sm)._trapExitCode;
    } else {
      try{
        @SuppressWarnings("unchecked")
        ThreadLocal<Boolean> res =
          (ThreadLocal<Boolean>) sm.getClass().getMethod("trapExitCode").invoke(null);
        return res;
      } catch(Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private final ThreadLocal<Boolean> _trapExitCode =
    new ThreadLocal<Boolean>() {
      @Override protected Boolean initialValue() {
        return false;
      }
    };

  public TrapSecurityManager(){
    super(NailgunLauncher.initialSecurityManager);
  }
  
  public void checkPermission( Permission permission ){
    /*
    NOTE: is it actually ok, to just make these empty?
    Calling .super leads to ClassNotFound exteption for a lambda.
    Calling to the previous SecurityManager leads to a stack overflow
    */
    if(!TrapSecurityManager.trapExitCode().get()){
      super.checkPermission(permission);
    }
  }
  public void checkPermission( Permission permission, Object context ){
    /* Does this methods need to be overidden? */
    if(!TrapSecurityManager.trapExitCode().get()){
      super.checkPermission(permission, context);
    }
  }

  private static final String prefix = "[TrappedExit] ";

  @Override
  public void checkExit( int status ){
    if(TrapSecurityManager.trapExitCode().get()){
      // using a RuntimeException and a prefix here instead of a custom
      // exception type because this is thrown by the installed TrapSecurityManager
      // but other versions of cbt need to be able to catch it, that do not have access
      // to that version of the TrapSecurityManager class
      throw new RuntimeException(prefix+status);
    }
    super.checkExit(status);
  }

  public static boolean isTrappedExit( Throwable t ){
    return t instanceof RuntimeException && t.getMessage().startsWith(prefix);
  }

  public static int exitCode( Throwable t ){
    assert(isTrappedExit(t));
    return Integer.parseInt( t.getMessage().substring(prefix.length()) );
  }

}
