package cbt;
import java.net.*;
import java.io.*;
import java.util.*;

public class MultiClassLoader2 extends ClassLoader{
  public ClassLoader[] parents;
  public ClassLoader[] parents(){
    return this.parents;
  }
  public MultiClassLoader2(ClassLoader... parents){
    super(null);
    this.parents = parents;
  }
  public Class findClass(String name) throws ClassNotFoundException{
    for(ClassLoader parent: parents){
      try{
        return parent.loadClass(name);
      } catch (ClassNotFoundException e) {
        if(e.getMessage() != name) throw e;
      }
    }
    // FIXME: have a logger in Java land
    // System.err.println("NOT FOUND: "+name);
    return null;
  }
  public URL findResource(String name){
    for(ClassLoader parent: parents){
      URL res = parent.getResource(name);
      if(res != null) return res;
    }
    return null;
  }
  public Enumeration<URL> findResources(String name) throws IOException{
    ArrayList<URL> resources = new ArrayList<URL>();
    for(ClassLoader parent: parents){
      for(URL resource: Collections.list(parent.getResources(name))){
         resources.add( resource );
      }
    }
    return Collections.enumeration(resources);
  }
  public String toString(){
    return super.toString() + "(" + Arrays.toString(parents) +")";
  }
}
