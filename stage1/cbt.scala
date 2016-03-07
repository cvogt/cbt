package cbt
import java.io._
import java.nio.file._
import java.net._
object `package`{
  private val lib = new BaseLib
  implicit class FileExtensionMethods( file: File ){
    def ++( s: String ): File = {
      if(s endsWith "/") throw new Exception(
        """Trying to append a String that ends in "/" to a File would loose it. Use .stripSuffix("/") if you need to."""
      )
      new File( file.toString ++ s )
    }
    def parent = lib.realpath(file ++ "/..")
    def string = file.toString
  }
  implicit class URLExtensionMethods( url: URL ){
    def ++( s: String ): URL = new URL( url.toString ++ s )
    def string = url.toString
  }
}
