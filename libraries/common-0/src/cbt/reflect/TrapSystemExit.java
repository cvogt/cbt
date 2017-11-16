package cbt.reflect;

import java.security.*;
import java.lang.reflect.InvocationTargetException;

public abstract class TrapSystemExit<T> {
  public static SecurityManager createSecurityManager(SecurityManager delegateTo) {
    return new TrapSecurityManager(delegateTo);
  }

  public static <T> T run(TrapSystemExit<T> runnable) throws Throwable {
    boolean trapExitCodeBefore = TrapSecurityManager.trapExitCode().get();
    try {
      TrapSecurityManager.trapExitCode().set(true);
      return runnable.run();
    } catch (InvocationTargetException exception) {
      Throwable cause = exception.getCause();
      if (TrapSecurityManager.isTrappedExit(cause)) {
        return runnable.wrap(TrapSecurityManager.exitCode(cause));
      }
      throw exception;
    } catch (Exception exception) {
      if (TrapSecurityManager.isTrappedExit(exception)) {
        return runnable.wrap(TrapSecurityManager.exitCode(exception));
      }
      throw exception;
    } finally {
      TrapSecurityManager.trapExitCode().set(trapExitCodeBefore);
    }
  }

  public abstract T run() throws Throwable;

  public abstract T wrap(int exitCode);
}
