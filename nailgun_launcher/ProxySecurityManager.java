package cbt;

import java.security.*;
import java.io.FileDescriptor;
import java.net.InetAddress;

/*
SecurityManager proxy that forwards all calls to the provided target if != null.
Useful to replace a previously installed SecurityManager, overriding some methods
but forwarding the rest.
*/
public class ProxySecurityManager extends SecurityManager{
  private SecurityManager target;
  public ProxySecurityManager(SecurityManager target){
    this.target = target;
  }
  public Object getSecurityContext() {
    if(target != null)
      return target.getSecurityContext();
    else return super.getSecurityContext();
  }
  public void checkPermission(Permission perm) {
    if(target != null) target.checkPermission(perm);
  }
  public void checkPermission(Permission perm, Object context) {
    if(target != null) target.checkPermission(perm, context);
  }
  public void checkCreateClassLoader() {
    if(target != null) target.checkCreateClassLoader();
  }
  public void checkAccess(Thread t) {
    if(target != null) target.checkAccess(t);
  }
  public void checkAccess(ThreadGroup g) {
    if(target != null) target.checkAccess(g);
  }
  public void checkExit(int status) {
    if(target != null) target.checkExit(status);
  }
  public void checkExec(String cmd) {
    if(target != null) target.checkExec(cmd);
  }
  public void checkLink(String lib) {
    if(target != null) target.checkLink(lib);
  }
  public void checkRead(FileDescriptor fd) {
    if(target != null) target.checkRead(fd);
  }
  public void checkRead(String file) {
    if(target != null) target.checkRead(file);
  }
  public void checkRead(String file, Object context) {
    if(target != null) target.checkRead(file, context);
  }
  public void checkWrite(FileDescriptor fd) {
    if(target != null) target.checkWrite(fd);
  }
  public void checkWrite(String file) {
    if(target != null) target.checkWrite(file);
  }
  public void checkDelete(String file) {
    if(target != null) target.checkDelete(file);
  }
  public void checkConnect(String host, int port) {
    if(target != null) target.checkConnect(host, port);
  }
  public void checkConnect(String host, int port, Object context) {
    if(target != null) target.checkConnect(host, port, context);
  }
  public void checkListen(int port) {
    if(target != null) target.checkListen(port);
  }
  public void checkAccept(String host, int port) {
    if(target != null) target.checkAccept(host, port);
  }
  public void checkMulticast(InetAddress maddr) {
    if(target != null) target.checkMulticast(maddr);
  }
  public void checkPropertiesAccess() {
    if(target != null) target.checkPropertiesAccess();
  }
  public void checkPropertyAccess(String key) {
    if(target != null) target.checkPropertyAccess(key);
  }
  public void checkPrintJobAccess() {
    if(target != null) target.checkPrintJobAccess();
  }
  public void checkPackageAccess(String pkg) {
    if(target != null) target.checkPackageAccess(pkg);
  }
  public void checkPackageDefinition(String pkg) {
    if(target != null) target.checkPackageDefinition(pkg);
  }
  public void checkSetFactory() {
    if(target != null) target.checkSetFactory();
  }
  public ThreadGroup getThreadGroup() {
    if(target != null)
      return target.getThreadGroup();
    else return super.getThreadGroup();
  }
}
