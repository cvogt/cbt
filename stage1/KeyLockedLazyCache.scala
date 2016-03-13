/*
package cbt
import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.Future

/**
A cache that lazily computes values if needed during lookup.
Locking occurs on the key, so separate keys can be looked up
simultaneously without a deadlock.
*/
final private[cbt] class KeyLockedLazyCache[Key <: AnyRef,Value]{
  private val keys = new ConcurrentHashMap[Key,LockableKey]()
  private val builds = new ConcurrentHashMap[LockableKey,Value]()

  private class LockableKey
  def get( key: Key, value: => Value ): Value = {
    val keyObject = keys.synchronized{    
      if( ! (keys containsKey key) ){
        keys.put( key, new LockableKey )
      }
      keys get key
    }
    // synchronizing on key only, so asking for a particular key does
    // not block the whole cache, but just that cache entry
    key.synchronized{
      if( ! (builds containsKey keyObject) ){
        builds.put( keyObject, value )
      }
      builds get keyObject      
    }
  }
}
*/
