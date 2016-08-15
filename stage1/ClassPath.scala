package cbt
import java.io._
import java.nio._
import java.nio.file._
import java.net._

object ClassPath{
  def flatten( classPaths: Seq[ClassPath] ): ClassPath = ClassPath( classPaths.map(_.files).flatten )
}
case class ClassPath(files: Seq[Path] = Seq()){
  private val duplicates = (files diff files.distinct).distinct
  assert(
    duplicates.isEmpty,
    "Duplicate classpath entries found:\n" ++ duplicates.mkString("\n") ++ "\nin classpath:\n"++string
  )
  private val nonExisting = files.distinct.filterNot( _.exists )
  assert(
    nonExisting.isEmpty,
    "Classpath contains entires that don't exist on disk:\n" ++ nonExisting.mkString("\n") ++ "\nin classpath:\n"++string
  )
  
  def +:(file: Path) = ClassPath(file +: files)
  def :+(file: Path) = ClassPath(files :+ file)
  def ++(other: ClassPath) = ClassPath(files ++ other.files)
  def string = strings.mkString( File.pathSeparator )
  def strings = files.map{
    f => f.toString ++ ( if( f.isDirectory ) "/" else "" )
  }.sorted
  def toConsole = string
}
