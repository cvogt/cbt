package cbt;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static java.io.File.pathSeparator;
import static cbt.Stage0Lib.*;

final class ClassLoaderCache2<T>{
  ConcurrentHashMap<String,Object> keys;
  ConcurrentHashMap<Object,T> values;

  public ClassLoaderCache2(
    ConcurrentHashMap<String,Object> keys,
    ConcurrentHashMap<Object,T> values
  ){
    this.keys = keys;
    this.values = values;
  }

  public T get( String key ){
    return values.get(
      keys.get( key )
    );
  }
  
  public Boolean contains( String key ){
    return keys.containsKey( key );
  }

  public T put( T value, String key ){
    LockableKey2 keyObject = new LockableKey2();
    keys.put( key, keyObject );
    values.put( keyObject, value );
    return value;
  }
}
class LockableKey2{}