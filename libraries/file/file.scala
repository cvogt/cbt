package cbt.file
import java.io.File
import java.nio.file.Files._
import java.nio.file.StandardCopyOption._
import cbt.common_1._
object `package` extends Module {
  implicit class CbtFileOps( val file: File ) extends ops.CbtFileOps
}

package ops {
  trait CbtFileOps extends Any {
    def file: File
    def ++( s: String ): File = {
      if ( s endsWith "/" ) throw new Exception(
        """Trying to append a String that ends in "/" to a File would loose the trailing "/". Use .stripSuffix("/") if you need to."""
      )
      new File( string + s ) // PERFORMANCE HOTSPOT
    }
    def /( s: String ): File = {
      new File( file, s )
    }
    def parent = realpath( file / ".." )
    def string = file.toString

    def listOrFail: Seq[File] = Option( file.listFiles ).getOrElse( throw new Exception( "no such file: " + file ) ).toVector
    def listRecursive: Seq[File] =
      file +: (
        if ( file.isDirectory ) file.listOrFail.flatMap( _.listRecursive ).toVector else Vector[File]()
      )

    def lastModifiedRecursive = listRecursive.map( _.lastModified ).max

    def readAsString = new String( readAllBytes( file.toPath ) )

    def quote = s"""new _root_.java.io.File(${string.quote})"""
  }
}

trait Module {
  def realpath( name: File ) = new File( java.nio.file.Paths.get( name.getAbsolutePath ).normalize.toString )
  def transformFiles( files: Seq[File], transform: String => String ): Seq[File] = {
    transformFilesOrError( files, s => Right( transform( s ) ) )._1
  }

  def transformFilesOrError[T]( files: Seq[File], transform: String => Either[T, String] ): ( Seq[File], Seq[( File, T )] ) = {
    val results = files.map { file =>
      val string = file.readAsString
      transform( string ).left.map(
        file -> _
      ).right.map(
          replaced =>
            if ( string != replaced ) {
              val tmpFile = file ++ ".cbt-tmp"
              assert( !tmpFile.exists )
              write( tmpFile.toPath, replaced.getBytes )
              move( tmpFile.toPath, file.toPath, REPLACE_EXISTING )
              Some( file )
            } else None
        )
    }

    ( results.map( _.right.toOption ).flatten.flatten, results.map( _.left.toOption ).flatten )
  }

  def autoRelative(
    files: Seq[File], collector: PartialFunction[( File, String ), String] = { case ( _, r ) => r },
    allowDuplicates: Boolean = false
  ): Seq[( File, String )] = {
    val map = files.sorted.flatMap { base =>
      val b = base.getCanonicalFile.string
      if ( base.isDirectory ) {
        base.listRecursive.map { f =>
          f -> f.getCanonicalFile.string.stripPrefix( b ).stripPrefix( File.separator )
        }
      } else {
        Seq( base -> base.getName )
      }
    }.collect {
      case v @ ( file, _ ) if collector.isDefinedAt( v ) => file -> collector( v )
    }
    if ( !allowDuplicates ) {
      val relatives = map.unzip._2
      val duplicateFiles = ( relatives diff relatives.distinct ).distinct
      assert(
        duplicateFiles.isEmpty, {
          val rs = relatives.toSet
          "Conflicting:\n\n" +
            map.filter( rs contains _._2 ).groupBy( _._2 ).mapValues( _.map( _._1 ).sorted ).toSeq.sortBy( _._1 ).map {
              case ( name, files ) => s"$name:\n" ++ files.mkString( "\n" )
            }.mkString( "\n\n" )
        }
      )
    }
    map
  }

  /* recursively deletes folders*/
  def deleteRecursive( file: File ): Unit = {
    val s = file.string
    // some desperate attempts to keep people from accidentally deleting their hard drive
    assert( file === file.getCanonicalFile, "deleteRecursive requires previous .getCanonicalFile" )
    assert( file.isAbsolute, "deleteRecursive requires absolute path" )
    assert( file.string != "", "deleteRecursive requires non-empty file path" )
    assert( s.split( File.separator.replace( "\\", "\\\\" ) ).size > 4, "deleteRecursive requires absolute path of at least depth 4" )
    assert( !file.listRecursive.exists( _.isHidden ), "deleteRecursive requires no files to be hidden" )
    assert( file.listRecursive.forall( _.canWrite ), "deleteRecursive requires all files to be writable" )
    if ( file.isDirectory ) {
      file.listOrFail.map( deleteRecursive )
    }
    file.delete
  }
}
