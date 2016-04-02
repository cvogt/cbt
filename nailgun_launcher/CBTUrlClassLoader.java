package cbt;
import java.io.*;
import java.net.*;
import java.util.*;
class CbtURLClassLoader extends java.net.URLClassLoader{
  public String toString(){
    return (
      super.toString()
      + "(\n  "
      + Arrays.toString(getURLs())
      + ",\n  "
      + NailgunLauncher.join("\n  ",getParent().toString().split("\n"))
      + "\n)"
    );
  }
  public Class loadClass(String name) throws ClassNotFoundException{
    //System.out.println("loadClass("+name+") on \n"+this);
    return super.loadClass(name);
  }
  void assertExist(URL[] urls){
    for(URL url: urls){
      if(!new File(url.getPath()).exists()){
        throw new AssertionError("File does not exist when trying to create CbtURLClassLoader: "+url);
      }
    }
  }
  public CbtURLClassLoader(URL[] urls, ClassLoader parent){
    super(urls, parent);
    assertExist(urls);
  }
  public CbtURLClassLoader(URL[] urls){
    super(urls);
    assertExist(urls);
  }
}