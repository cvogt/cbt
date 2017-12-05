package cbt.process
import cbt.ExitCode
import java.io._

object `package` extends Module

trait Module {
  def runMainForked(
    className: String,
    args:      Seq[String],
    classpath: String,
    directory: Option[File],
    outErrIn:  Option[( OutputStream, OutputStream, InputStream )]
  ): ( Int, () => ExitCode, () => ExitCode ) = {
    // FIXME: Windows support
    val java_exe = new File( System.getProperty( "java.home" ) + "/bin/java" )
    runWithIO(
      java_exe.toString +: "-cp" +: classpath +: className +: args,
      directory,
      outErrIn
    )
  }

  def runWithIO(
    commandLine: Seq[String],
    directory:   Option[File],
    outErrIn:    Option[( OutputStream, OutputStream, InputStream )]
  ): ( Int, () => ExitCode, () => ExitCode ) = {
    val pb = new ProcessBuilder( commandLine: _* )
    outErrIn.map {
      case ( out, err, in ) =>
        val process = directory.map( pb.directory( _ ) ).getOrElse( pb )
          .redirectInput( ProcessBuilder.Redirect.PIPE )
          .redirectOutput( ProcessBuilder.Redirect.PIPE )
          .redirectError( ProcessBuilder.Redirect.PIPE )
          .start

        (
          processId( process ),
          () => {
            val lock = new AnyRef

            val t1 = asyncPipeCharacterStreamSyncLines( process.getErrorStream, err, lock )
            val t2 = asyncPipeCharacterStreamSyncLines( process.getInputStream, out, lock )
            val t3 = asyncPipeCharacterStream( System.in, process.getOutputStream, process.isAlive )

            t1.start
            t2.start
            t3.start

            t1.join
            t2.join

            val e = process.waitFor
            System.err.println( scala.Console.RESET + "Please press ENTER to continue..." )
            t3.join
            ExitCode( e )
          },
          () => {
            process.destroy
            Thread.sleep( 20 )
            ExitCode( process.destroyForcibly.waitFor )
          }
        )
    }.getOrElse {
      val process = pb.inheritIO.start
      (
        processId( process ),
        () => ExitCode( process.waitFor ),
        () => {
          process.destroy
          Thread.sleep( 20 )
          ExitCode( process.destroyForcibly.waitFor )
        }
      )
    }
  }

  private def accessField( cls: Class[_], field: String ): java.lang.reflect.Field = {
    val f = cls.getDeclaredField( field )
    f.setAccessible( true )
    f
  }

  import com.sun.jna.{ Library, Native, Platform }
  private trait CLibrary extends Library {
    def getpid: Int
  }
  val nativeLib = if(Platform.isWindows()) "msvcrt" else "c"
  private val CLibraryInstance: CLibrary = Native.loadLibrary( nativeLib, classOf[CLibrary] ).asInstanceOf[CLibrary]

  def currentProcessId: Int = {
    if ( Option( System.getProperty( "os.name" ) ).exists( _.startsWith( "Windows" ) ) ) {
      com.sun.jna.platform.win32.Kernel32.INSTANCE.GetCurrentProcessId
    } else {
      CLibraryInstance.getpid
    }
  }

  /** process id of given Process */
  def processId( process: Process ): Int = {
    val clsName = process.getClass.getName
    if ( clsName == "java.lang.UNIXProcess" ) {
      accessField( process.getClass, "pid" ).getInt( process )
    } else if ( clsName == "java.lang.Win32Process" || clsName == "java.lang.ProcessImpl" ) {
      import com.sun.jna.platform.win32.{ WinNT, Kernel32 }
      val handle = new WinNT.HANDLE
      handle.setPointer(
        com.sun.jna.Pointer.createConstant(
          accessField( process.getClass, "handle" ).getLong( process )
        )
      )
      Kernel32.INSTANCE.GetProcessId( handle )
    } else {
      throw new Exception( "Unexpected Process sub-class: " + clsName )
    }
  }

  def asyncPipeCharacterStreamSyncLines( inputStream: InputStream, outputStream: OutputStream, lock: AnyRef ): Thread = {
    new Thread(
      new Runnable {
        def run = {
          val b = new BufferedInputStream( inputStream )
          Iterator.continually {
            b.read // block until and read next character
          }.takeWhile( _ != -1 ).map { c =>
            lock.synchronized { // synchronize with other invocations
              outputStream.write( c )
              Iterator
                .continually( b.read )
                .takeWhile( _ != -1 )
                .map { c =>
                  try {
                    outputStream.write( c )
                    outputStream.flush
                    (
                      c != '\n' // release lock when new line was encountered, allowing other writers to slip in
                      && b.available > 0 // also release when nothing is available to not block other outputs
                    )
                  } catch {
                    case e: IOException if e.getMessage == "Stream closed" => false
                  }
                }
                .takeWhile( identity )
                .length // force entire iterator
            }
          }.length // force entire iterator
        }
      }
    )
  }

  def asyncPipeCharacterStream( inputStream: InputStream, outputStream: OutputStream, continue: => Boolean ) = {
    new Thread(
      new Runnable {
        def run = {
          Iterator
            .continually { inputStream.read }
            .takeWhile( _ != -1 )
            .map { c =>
              try {
                outputStream.write( c )
                outputStream.flush
                true
              } catch {
                case e: IOException if e.getMessage == "Stream closed" => false
              }
            }
            .takeWhile( identity )
            .takeWhile( _ => continue )
            .length // force entire iterator
        }
      }
    )
  }
}
