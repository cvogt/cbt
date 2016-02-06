package cbt
/**
Caches exactly one value
Is there a less boiler-platy way to achieve this, that doesn't
require creating an instance for each thing you want to cache?
*/
class Cache[T]{
  private var value: Option[T] = None
  def apply(value: => T) = this.synchronized{
    if(!this.value.isDefined)
      this.value = Some(value)
    this.value.get
  }
}
