package cbt;

import java.util.*;
import static java.io.File.pathSeparator;
import static cbt.Stage0Lib.*;

final class JavaCache<T>{
  Map<Object,Object> hashMap;

  public JavaCache(
    Map<Object,Object> hashMap
  ){
    this.hashMap = hashMap;
  }

  public T get( Object key ){
    @SuppressWarnings("unchecked")
    T t = (T) hashMap.get(
      hashMap.get( key )
    );
    return t;
  }

  public Boolean contains( Object key/*, Long timestamp*/ ){
    return hashMap.containsKey( key );/* && (
      (Long) hashMap.get( hashMap.get( hashMap.get(key) ) ) >= timestamp
    );*/
  }

  public T put( Object value, Object key/*, Long timestamp*/ ){
    LockableJavaKey keyObject = new LockableJavaKey();
    hashMap.put( key, keyObject );
    hashMap.put( keyObject, value );
    //hashMap.put( value, timestamp );
    @SuppressWarnings("unchecked")
    T t = (T) value;
    return t;
  }
}
class LockableJavaKey{}
