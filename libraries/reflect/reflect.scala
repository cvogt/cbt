package cbt.reflect

import java.io.File
import java.lang.reflect.{ Constructor, Method, InvocationTargetException, Modifier }

import scala.reflect.ClassTag

import cbt.ExitCode
import cbt.file._
import cbt.common_1._

object `package` extends Module {
  implicit class CbtClassOps( val c: Class[_] ) extends AnyVal with ops.CbtClassOps
  implicit class CbtConstructorOps( val c: Constructor[_] ) extends AnyVal with ops.CbtConstructorOps
  implicit class CbtMethodOps( val m: Method ) extends AnyVal with ops.CbtMethodOps
}

package ops {
  trait CbtClassOps extends Any {
    def c: Class[_]
    def name = c.getName
    def method( name: String, parameterTypes: Class[_]* ) = c.getMethod( name, parameterTypes: _* )
    def methods = c.getMethods
    def modifiers = c.getModifiers
    def constructors = c.getConstructors
    def declaredMethods = c.getDeclaredMethods
    def isInterface = Modifier.isInterface( c.getModifiers )
    def isAbstract = Modifier.isAbstract( c.getModifiers )
    def isPrivate = Modifier.isPrivate( c.getModifiers )
    def isProtected = Modifier.isProtected( c.getModifiers )
    def isPublic = Modifier.isPublic( c.getModifiers )
    def isFinal = Modifier.isFinal( c.getModifiers )
  }
  trait CbtConstructorOps extends Any {
    def c: Constructor[_]
    def parameterTypes = c.getParameterTypes
  }
  trait CbtMethodOps extends Any {
    def m: Method
    def name = m.getName
    def declaringClass = m.getDeclaringClass
    def modifiers = m.getModifiers
    def parameters = m.getParameters
    def parameterTypes = m.getParameterTypes
    def returnType = m.getReturnType

    def isAbstract = Modifier.isAbstract( m.getModifiers )
    def isFinal = Modifier.isFinal( m.getModifiers )
    def isNative = Modifier.isNative( m.getModifiers )
    def isPrivate = Modifier.isPrivate( m.getModifiers )
    def isProtected = Modifier.isProtected( m.getModifiers )
    def isPublic = Modifier.isPublic( m.getModifiers )
    def isStatic = Modifier.isStatic( m.getModifiers )
    def isStrict = Modifier.isStrict( m.getModifiers )
    def isSynchronized = Modifier.isSynchronized( m.getModifiers )
    def isTransient = Modifier.isTransient( m.getModifiers )
    def isVolatile = Modifier.isVolatile( m.getModifiers )

    def show = (
      m.name ~ "( "
      ~ m.parameters.map( _.getType.name ).mkString( ", " )
      ~ " )"
    )
  }
}
trait Module {
  def getMain( cls: Class[_] ): StaticMethod[Seq[String], ExitCode] = {
    val f = findStaticExitMethodOrFail[Array[String]]( cls, "main" )
    f.copy(
      function = ( args: Seq[String] ) => f.function( args.to )
    )
  }

  def findMain( cls: Class[_] ): Option[StaticMethod[Seq[String], ExitCode]] = {
    findStaticExitMethod[Array[String]]( cls, "main" )
      .map( f =>
        f.copy(
          function = ( args: Seq[String] ) => f.function( args.to )
        ) )
  }

  /** ignoreMissingClasses allows ignoring other classes root directories which are subdirectories of this one */
  def topLevelClasses(
    classesRootDirectory: File,
    classLoader:          ClassLoader,
    ignoreMissingClasses: Boolean
  ): Seq[Class[_]] =
    topLevelClassNames( classesRootDirectory )
      .map { name =>
        try {
          classLoader.loadClass( name )
        } catch {
          case e: ClassNotFoundException if ignoreMissingClasses => null
          case e: NoClassDefFoundError if ignoreMissingClasses   => null
        }
      }
      .filterNot( ignoreMissingClasses && _ == null )

  /** Given a directory corresponding to the root package, return
    * the names of all top-level classes based on the class files found
    */
  def topLevelClassNames( classesRootDirectory: File ): Seq[String] =
    classesRootDirectory.listRecursive
      .filter( _.isFile )
      .map( _.getPath )
      .collect {
        // no $ to avoid inner classes
        case path if !path.contains( "$" ) && path.endsWith( ".class" ) =>
          path
            .stripSuffix( ".class" )
            .stripPrefix( classesRootDirectory.getPath )
            .stripPrefix( File.separator ) // 1 for the slash
            .replace( File.separator, "." )
      }

  def findStaticExitMethodOrFail[Arg: ClassTag](
    cls: Class[_], name: String
  ): StaticMethod[Arg, ExitCode] = {
    val f = findStaticMethodOrFail[Arg, Unit]( cls, name )
    f.copy(
      function = arg => trapExitCode { f.function( arg ); ExitCode.Success }
    )
  }

  def findStaticMethodOrFail[Arg, Result](
    cls: Class[_], name: String
  )(
    implicit
    Result: ClassTag[Result], Arg: ClassTag[Arg]
  ): StaticMethod[Arg, Result] = {
    val m = cls.method( name, Arg.runtimeClass )
    assert( Result.runtimeClass.isAssignableFrom( m.returnType ) )
    typeStaticMethod( m ).get // This will fail if None is returned.
  }

  def findStaticExitMethod[Arg: ClassTag](
    cls: Class[_], name: String
  ): Option[StaticMethod[Arg, ExitCode]] =
    findStaticMethod[Arg, Unit]( cls, name ).map( f =>
      f.copy(
        function = arg => trapExitCode { f.function( arg ); ExitCode.Success }
      ) )

  def findStaticMethod[Arg, Result](
    cls: Class[_], name: String
  )(
    implicit
    Result: ClassTag[Result], Arg: ClassTag[Arg]
  ): Option[StaticMethod[Arg, Result]] = {
    Some( cls )
      .filterNot( _.isAbstract )
      .filterNot( _.isInterface )
      .flatMap( _
        .getMethods
        .find( m =>
          !m.isAbstract
            && m.isPublic
            && m.name == name
            && m.parameterTypes.toList == List( Arg.runtimeClass )
            && Result.runtimeClass.isAssignableFrom( m.returnType ) ) )
      .flatMap( typeStaticMethod )
  }

  def typeStaticMethod[Arg, Result]( method: Method ): Option[StaticMethod[Arg, Result]] = {
    val m = method
    val instanceOption =
      if ( m.isStatic ) Some(null)
      else m.declaringClass.getConstructors.find( _.getParameterCount == 0 ).map( _.newInstance() ) // Dottydoc needs this. It's main method is not static.
    instanceOption.map( instance => StaticMethod(
      arg => m.invoke( instance, arg.asInstanceOf[AnyRef] ).asInstanceOf[Result],
      m
    ))
  }

  def trapExitCodeOrValue[T]( result: => T, i: Int = 5 ): Either[ExitCode, T] = {
    TrapSystemExit.run(
      new TrapSystemExit[Either[ExitCode, T]] {
        def run = Right( result )
        def wrap( exitCode: Int ) = Left( new ExitCode( exitCode ) )
      }
    )
  }

  def trapExitCode( code: => ExitCode ): ExitCode =
    trapExitCodeOrValue( code ).merge
}
