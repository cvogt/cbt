package cbt

private[cbt] class LockableKey
/**
A hashMap that lazily computes values if needed during lookup.
Locking occurs on the key, so separate keys can be looked up
simultaneously without a deadlock.
*/
final private[cbt] class KeyLockedLazyCache[T <: AnyRef](
  val hashMap: java.util.Map[AnyRef,AnyRef],
  logger: Option[Logger]
){
  final val seen = new ThreadLocal[collection.mutable.Set[AnyRef]](){
    override protected def initialValue = collection.mutable.Set[AnyRef]();
  }
  def get( key: AnyRef, value: => T ): T = {
    seen.get.add( key );
    val lockableKey = hashMap.synchronized{
      if( ! (hashMap containsKey key) ){
        val lockableKey = new LockableKey
        //logger.foreach(_.resolver("CACHE MISS: " ++ key.toString))
        hashMap.put( key, lockableKey )
        lockableKey
      } else {
        val lockableKey = hashMap get key
        //logger.foreach(_.resolver("CACHE HIT: " ++ lockableKey.toString ++ " -> " ++ key.toString))
        lockableKey
      }
    }
    import collection.JavaConversions._
    //logger.resolver("CACHE: \n" ++ hashMap.mkString("\n"))
    // synchronizing on key only, so asking for a particular key does
    // not block the whole hashMap, but just that hashMap entry
    lockableKey.synchronized{
      if( ! (hashMap containsKey lockableKey) ){
        hashMap.put( lockableKey, value )
      }
      (hashMap get lockableKey).asInstanceOf[T]
    }
  }
  def update( key: AnyRef, value: T ): T = {
    assert(
      !seen.get.contains( key ),
      "Thread tries to update cache key after observing it: " + key
    )
    val lockableKey = hashMap get key
    lockableKey.synchronized{
      hashMap.put( lockableKey, value )
      value
    }
  }
  def remove( key: AnyRef ) = hashMap.synchronized{
    assert(hashMap containsKey key)
    val lockableKey = hashMap get key
    lockableKey.synchronized{
      if(hashMap containsKey lockableKey){
        // this is so hashMap in the process of being replaced (which mean they have a key but no value)
        // are not being removed
        hashMap.remove( key )
        hashMap.remove( lockableKey )
      }
    }
  }
  def containsKey( key: AnyRef ) = hashMap.synchronized{
    hashMap containsKey key
  }
}
