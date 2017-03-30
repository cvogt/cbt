package cbt
import java.io.File
/** You likely want to use the factory method in the BasicBuild class instead of this. */
object DirectoryDependency {
  def apply( path: File, subBuild: String )( implicit context: Context ): LazyDependency =
    apply( path, Some( subBuild ) )
  def apply( path: File, subBuild: Option[String] = None )( implicit context: Context ): LazyDependency =
    apply( context.copy( workingDirectory = path ), subBuild )

  def apply( context: Context, subBuild: Option[String] ): LazyDependency = {
    val lib: Lib = new Lib( context.logger )
    val d = context.workingDirectory
    val relativeBuildFile = lib.buildDirectoryName ++ File.separator ++ lib.buildFileName
    val buildDirectory = d / lib.buildDirectoryName
    val buildFile = buildDirectory / lib.buildFileName

    if ( !buildFile.isFile && ( buildDirectory / "Build.scala" ).isFile ) {
      throw new Exception(
        s"""expected $relativeBuildFile but found ${lib.buildDirectoryName ++ File.separator ++ "Build.scala"} in $d"""
      )
    }

    if ( buildDirectory.exists && buildDirectory.listOrFail.nonEmpty && !buildFile.exists ) {
      throw new Exception(
        s"No file ${lib.buildFileName} (lower case) found in " ++ buildDirectory.string
      )
    }

    def loadBuild: AnyRef = {
      val actualBuildFile = (
        buildFile.getParentFile.getCanonicalFile.getName ++ File.separator ++ buildFile.getCanonicalFile.getName
      )
      if ( actualBuildFile =!= relativeBuildFile ) {
        throw new Exception( s"expected $relativeBuildFile but found $actualBuildFile in " ++ context.workingDirectory.string )
      }

      if ( buildFile.isFile ) (
        loadCustomBuildWithDifferentCbtVersion
        getOrElse loadCustomBuild
      )
      else if ( d.getCanonicalFile.getName === lib.buildDirectoryName && ( d / lib.buildFileName ).exists )
        new cbt.ConcreteBuildBuild( context )
      else
        new BasicBuild( context )
    }

    def loadCustomBuildWithDifferentCbtVersion: Option[AnyRef] = {
      ( "cbt:" ++ GitDependency.GitUrl.regex ++ "#[a-z0-9A-Z]+" ).r
        .findFirstIn( buildFile.readAsString )
        .map( _.drop( 4 ).split( "#" ) )
        .flatMap {
          case Array( base, hash ) =>
            val sameCbtVersion = context.cbtHome.string.contains( hash )
            if ( sameCbtVersion ) None else Some {
              // Note: cbt can't use an old version of itself for building,
              // otherwise we'd have to recursively build all versions since
              // the beginning. Instead CBT always needs to build the pure Java
              // Launcher in the checkout with itself and then run it via reflection.
              val ( checkoutDirectory, dependency ) =
                GitDependency.withCheckoutDirectory( base, hash, Some( "nailgun_launcher" ) )(
                  context.copy( scalaVersion = None )
                )
              dependency
                .dependency
                .asInstanceOf[BaseBuild] // should work because nailgun_launcher/ has no cbt build of it's own
                .classLoader
                .loadClass( "cbt.NailgunLauncher" )
                .getMethod( "getBuild", classOf[AnyRef] )
                .invoke( null, context.copy( cbtHome = checkoutDirectory ) )
            }
        }
    }

    def loadCustomBuild: AnyRef = {
      lib.logger.composition( "Loading build at " ++ buildDirectory.string )
      val buildBuild = apply(
        context.copy( workingDirectory = buildDirectory, scalaVersion = None ), None
      ).dependency.asInstanceOf[BuildInterface]
      import buildBuild._
      val managedContext = context.copy( parentBuild = Some( buildBuild ) )

      val buildClasses =
        buildBuild.exportedClasspath.files.flatMap(
          lib.topLevelClasses( _, classLoader, false )
            .filter( _.getSimpleName === lib.buildClassName )
            .filter( classOf[BaseBuild] isAssignableFrom _ )
        )

      if ( buildClasses.size === 0 ) {
        throw new Exception(
          s"You need to define a class ${lib.buildClassName} extending an appropriate super class in\n"
            + buildFile ++ "\nbut none found."
        )
      } else if ( buildClasses.size > 1 ) {
        throw new Exception(
          s"You need to define exactly one class ${
            lib.buildClassName
          } extending an appropriate build super class, but multiple found in "
            + buildDirectory + ":\n" + buildClasses.mkString( "\n" )
        )
      } else {
        val buildClass = buildClasses.head
        buildClass.constructors.find( _.parameterTypes.toList === List( classOf[Context] ) ).map {
          _.newInstance( managedContext ).asInstanceOf[AnyRef]
        }.getOrElse {
          throw new Exception(
            s"Expected class ${lib.buildClassName}(val context: Context), but found different constructor in\n"
              + buildDirectory ++ "\n"
              + buildClass ++ "(" ++ buildClass.getConstructors.map( _.getParameterTypes.mkString( ", " ) ).mkString( "; " ) + ")"
          )
        }
      }
    }

    // wrapping this in lazy so builds are not loaded from disk immediately
    // this definitely has the advantages that cbt can detect cycles in the
    // dependencies, but might also positively affect performance
    new LazyDependency( {
      val build = lib.getReflective( loadBuild, subBuild )( context )
      try {
        build.asInstanceOf[Dependency]
      } catch {
        case e: ClassCastException =>
          throw new RuntimeException( "Your class " ++ lib.buildClassName ++ " needs to extend class BaseBuild in $buildFile", e )
      }
    } )( context.logger, context.transientCache, context.classLoaderCache )
  }
}
