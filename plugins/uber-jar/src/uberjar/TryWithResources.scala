package cbt.uberjar

import java.io.Closeable

import scala.util.Try

private[cbt] object TryWithResources {
  def withCloseable[T <: Closeable, R](t: T)(f: T => R): Try[R] = {
    val result = Try(f(t))
    t.close()
    result
  }
}
