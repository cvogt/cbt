package cbt

import java.util.concurrent.ConcurrentHashMap

private[cbt] class LockableKey
/**
A cache that lazily computes values if needed during lookup.
Locking occurs on the key, so separate keys can be looked up
simultaneously without a deadlock.
*/
final private[cbt] class KeyLockedLazyCache[Key <: AnyRef,Value <: AnyRef](
  val keys: ConcurrentHashMap[Key,AnyRef],
  val values: ConcurrentHashMap[AnyRef,Value],
  logger: Option[Logger]
){
  def get( key: Key, value: => Value ): Value = {
    val lockableKey = keys.synchronized{
      if( ! (keys containsKey key) ){
        val lockableKey = new LockableKey
        //logger.foreach(_.resolver("CACHE MISS: " ++ key.toString))
        keys.put( key, lockableKey )
        lockableKey
      } else {
        val lockableKey = keys get key
        //logger.foreach(_.resolver("CACHE HIT: " ++ lockableKey.toString ++ " -> " ++ key.toString))
        lockableKey
      }
    }
    import collection.JavaConversions._
    //logger.resolver("CACHE: \n" ++ keys.mkString("\n"))
    // synchronizing on key only, so asking for a particular key does
    // not block the whole cache, but just that cache entry
    lockableKey.synchronized{
      if( ! (values containsKey lockableKey) ){
        values.put( lockableKey, value )
      }
      values get lockableKey      
    }
  }
  def update( key: Key, value: Value ): Value = {
    val lockableKey = keys get key
    lockableKey.synchronized{
      values.put( lockableKey, value )
      value
    }
  }
  def remove( key: Key ) = keys.synchronized{    
    assert(keys containsKey key)
    val lockableKey = keys get key
    lockableKey.synchronized{
      if(values containsKey lockableKey){
        // this is so values in the process of being replaced (which mean they have a key but no value)
        // are not being removed
        keys.remove( key )
        values.remove( lockableKey )  
      }
    }
  }
  def containsKey( key: Key ) = keys.synchronized{
    keys containsKey key
  }
}
