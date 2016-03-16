package cbt;
import java.io.*;
import java.net.*;
import java.util.*;
class CbtURLClassLoader extends URLClassLoader{
  public String toString(){
    return (
      super.toString()
      + "(\n  "
      + Arrays.toString(getURLs())
      + ",\n  "
      + String.join("\n  ",getParent().toString().split("\n"))
      + "\n)"
    );
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