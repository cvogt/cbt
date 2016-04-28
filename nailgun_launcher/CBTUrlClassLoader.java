package cbt;
import java.io.*;
import java.net.*;
import java.util.*;
import static cbt.Stage0Lib.*;
import java.util.concurrent.ConcurrentHashMap;
class CbtURLClassLoader extends java.net.URLClassLoader{
  public String toString(){
    return (
      super.toString()
      + "(\n  "
      + Arrays.toString(getURLs())
      + ",\n  "
      + join("\n  ",(getParent() == null?"":getParent().toString()).split("\n"))
      + "\n)"
    );
  }
  ClassLoaderCache2<Class> cache = new ClassLoaderCache2<Class>(
    new ConcurrentHashMap<String, Object>(),
    new ConcurrentHashMap<Object, Class>()
  );
  public Class loadClass(String name) throws ClassNotFoundException{
    Class _class = super.loadClass(name);
    if(_class == null) throw new ClassNotFoundException(name);
    else return _class;
  }
  public Class loadClass(String name, Boolean resolve) throws ClassNotFoundException{
    //System.out.println("loadClass("+name+") on \n"+this);
    if(!cache.contains(name))
      try{
        cache.put(super.loadClass(name, resolve), name);
      } catch (ClassNotFoundException e){
        cache.put(Object.class, name);
      }
    Class _class = cache.get(name);
    if(_class == Object.class){
      if( name == "java.lang.Object" )
        return Object.class;
      else return null;
    } else {
      return _class;
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