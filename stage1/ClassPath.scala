package cbt
import java.io._
import java.net._

object ClassPath{
  def flatten( classPaths: Seq[ClassPath] ): ClassPath = ClassPath( classPaths.map(_.files).flatten )
}
case class ClassPath(files: Seq[File] = Seq()){
  private val duplicates = (files diff files.distinct).distinct
  assert(
    duplicates.isEmpty,
    "Duplicate classpath entries found:\n" ++ duplicates.mkString("\n") ++ "\nin classpath:\n"++string
  )
  private val nonExisting = files.distinct.filterNot(_.exists)
  assert(
    nonExisting.isEmpty,
    "Classpath contains entires that don't exist on disk:\n" ++ nonExisting.mkString("\n") ++ "\nin classpath:\n"++string
  )

  def +:(file: File) = ClassPath(file +: files)
  def :+(file: File) = ClassPath(files :+ file)
  def ++(other: ClassPath) = ClassPath(files ++ other.files)
  def string = strings.mkString( File.pathSeparator )
  def strings = files.map{
    f => f.string + (
      // using file extension instead of isDirectory for performance reasons
      if( f.getName.endsWith(".jar") /* !f.isDirectory */ ) "" else "/"
    )
  }.sorted

  def verify( lib: Stage1Lib ) = {
    val ( directories, jarFiles ) = files.partition(_.isDirectory)
    val all = lib.autoRelative(
      directories
    ) ++ jarFiles.flatMap{ f => 
      import collection.JavaConverters._
      new java.util.jar.JarFile(f).entries.asScala.filterNot(_.isDirectory).toVector.map(
        f -> _.toString
      )
    }
    val duplicates =
      all
        .groupBy( _._2 )
        .filter( _._2.size > 1 )
        .filter( _._1 endsWith ".class" )
        .mapValues( _.map( _._1 ) )
        .toSeq
        .sortBy( _._1 )
    assert(
      duplicates.isEmpty,
      {
        "Conflicting:\n\n" +
        duplicates.map{
          case ( path, locations ) =>
            path + " exits in multiple locations:\n" + locations.mkString("\n")
        }.mkString("\n\n")
      }
    )
  }
}
