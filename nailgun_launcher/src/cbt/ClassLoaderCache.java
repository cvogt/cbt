package cbt;

import java.util.*;
import static java.io.File.pathSeparator;
import static cbt.Stage0Lib.*;

final public class ClassLoaderCache{
  public Map<Object,Object> hashMap;
  final ThreadLocal<HashSet<String>> seen = new ThreadLocal<HashSet<String>>(){
    @Override protected HashSet<String> initialValue(){
      return new HashSet<String>();
    }
  };

  public ClassLoaderCache(
    Map<Object,Object> hashMap
  ){
    this.hashMap = hashMap;
  }

  public ClassLoader get( String key, long timestamp ){
    seen.get().add( key );
    @SuppressWarnings("unchecked")
    ClassLoader t = (ClassLoader) hashMap.get(
      hashMap.get( key )
    );
    assert hashMap.get(t).equals(timestamp);
    return t;
  }

  public boolean containsKey( String key, long timestamp ){
    boolean contains = hashMap.containsKey( key );
    if( contains ){
      Object keyObject = hashMap.get( key );
      Object classLoader = hashMap.get( keyObject );
      long oldTimestamp = (long) hashMap.get( classLoader );
      boolean res = oldTimestamp == timestamp;
      return res;
    } else {
      return false;
    }
  }

  public void put( String key, ClassLoader value, long timestamp ){
    assert !seen.get().contains( key ): "Thread tries to update cache key after observing it: " + key;
    LockableJavaKey keyObject = new LockableJavaKey();
    hashMap.put( key, keyObject );
    hashMap.put( keyObject, value );
    hashMap.put( value, timestamp );
  }

  @Override public String toString(){
    StringBuilder res = new StringBuilder();
    res.append("ClassLoaderCache(\n\n");
    for( Object key: hashMap.keySet() ){
      if( key instanceof String )
        res.append(
          mkString( "\n", key.toString().split(":") ) + " -> " + hashMap.get( hashMap.get(key) )
          + "\n\n"
        );
    }
    res.append("\n\n");
    return res.toString();
  }
}
class LockableJavaKey{}
