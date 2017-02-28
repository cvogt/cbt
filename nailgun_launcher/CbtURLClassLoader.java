package cbt;
import java.io.*;
import java.net.*;
import java.util.*;
import static cbt.Stage0Lib.*;
import java.util.concurrent.ConcurrentHashMap;
public class CbtURLClassLoader extends java.net.URLClassLoader{
  public String toString(){
    return (
      super.toString()
      + "(\n  "
      + Arrays.toString(getURLs())
      + ",\n  "
      + mkString("\n  ",(getParent() == null?"":getParent().toString()).split("\n"))
      + "\n)"
    );
  }
  ConcurrentHashMap<Object,Object> cache = new ConcurrentHashMap<Object,Object>();
  public Class loadClass(String name) throws ClassNotFoundException{
    Class _class = super.loadClass(name);
    if(_class == null) throw new ClassNotFoundException(name);
    else return _class;
  }
  public Class loadClass(String name, boolean resolve) throws ClassNotFoundException{
    //System.out.println("loadClass("+name+") on \n"+this);
    synchronized( cache ){
      if(!cache.containsKey(name))
        cache.put(name, new Object());
    }
    Object key = cache.get(name);
    synchronized( key ){
      if(!cache.containsKey(key)){
        try{
          cache.put(key, super.loadClass(name, resolve));
        } catch (ClassNotFoundException e){
          cache.put(key, Object.class);
        }
      }
      Class _class = (Class) cache.get(key);
      if(_class == Object.class){
        if( name.equals("java.lang.Object") )
          return Object.class;
        else return null;
      } else {
        return _class;
      }
    }
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
    super(urls, null);
    assertExist(urls);
  }
}