package cbt
import java.io._
import java.net._
import scala.collection.immutable.Seq

object ClassPath{
  def apply(files: File*): ClassPath = ClassPath(files.toVector)
  def flatten( classPaths: Seq[ClassPath] ): ClassPath = ClassPath( classPaths.map(_.files).flatten )
}
case class ClassPath(files: Seq[File]){
  private val duplicates = (files diff files.distinct).distinct
  assert(
    duplicates.isEmpty,
    "Duplicate classpath entries found:\n" ++ duplicates.mkString("\n") ++ "\nin classpath:\n"++string
  )
  private val nonExisting = files.distinct.filterNot(_.exists)
  assert(
    duplicates.isEmpty,
    "Classpath contains entires that don't exist on disk:\n" ++ nonExisting.mkString("\n") ++ "\nin classpath:\n"++string
  )
  
  def +:(file: File) = ClassPath(file +: files)
  def :+(file: File) = ClassPath(files :+ file)
  def ++(other: ClassPath) = ClassPath(files ++ other.files)
  def string = strings.mkString( File.pathSeparator )
  def strings = files.map{
    f => f.string ++ ( if(f.isDirectory) "/" else "" )
  }
  def toConsole = string
}
