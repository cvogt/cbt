package cbt
package proguard
import java.io.File
import java.nio.file.Files.deleteIfExists

sealed class KeepOptionModifier( val string: String )
object KeepOptionModifier {
  /* AUTO GENERATED SECTION BEGIN: keepModifiers */
  /** Also keep any classes in the descriptors of specified fields and methods. */
  object includedescriptorclasses extends KeepOptionModifier( "includedescriptorclasses" )

  /** Allow the specified entry points to be removed in the shrinking step. */
  object allowshrinking extends KeepOptionModifier( "allowshrinking" )

  /** Allow the specified entry points to be modified in the optimization step. */
  object allowoptimization extends KeepOptionModifier( "allowoptimization" )

  /** Allow the specified entry points to be renamed in the obfuscation step. */
  object allowobfuscation extends KeepOptionModifier( "allowobfuscation" )
  /* AUTO GENERATED SECTION END: keepModifiers */
}

object ProGuard {
  val artifactId = "proguard-base"
  val groupId = "net.sf.proguard"
  val version = "5.3.2"
  val mainClass = "proguard.ProGuard"
  val `rt.jar` = Seq( new File( System.getProperty( "java.home" ), "lib/rt.jar" ) )
}
case class ProGuard[T](
  main: Seq[String] => Int,
  T:    Seq[File] => T,
  log:  String => Unit = _ => ()
) {

  /**
  Typed interface on top of the proguard command line tool.
  Check the official ProGuard docs for usage.
  Use `Some(  None )` to call an option without arguments.
  Use `true` to set a flag.

  @see https://www.guardsquare.com/en/proguard/manual/refcard
  @see https://www.guardsquare.com/en/proguard/manual/usage

  /* AUTO GENERATED SECTION BEGIN: docs */
  @param include Read configuration options from the given file.
  @param basedirectory Specifies the base directory for subsequent relative file names.
  @param injars Specifies the program jars ( or wars, ears, zips, or directories ).
  @param outjars Specifies the names of the output jars ( or wars, ears, zips, or directories ).
  @param libraryjars Specifies the library jars ( or wars, ears, zips, or directories ).
  @param skipnonpubliclibraryclasses Ignore non-public library classes.
  @param dontskipnonpubliclibraryclasses Don't ignore non-public library classes ( the default ).
  @param dontskipnonpubliclibraryclassmembers Don't ignore package visible library class members.
  @param keepdirectories Keep the specified directories in the output jars ( or wars, ears, zips, or directories ).
  @param target Set the given version number in the processed classes.
  @param forceprocessing Process the input, even if the output seems up to date.
  @param keep Preserve the specified classes and class members.
  @param keepclassmembers Preserve the specified class members, if their classes are preserved as well.
  @param keepclasseswithmembers Preserve the specified classes and class members, if all of the specified class members are present.
  @param keepnames Preserve the names of the specified classes and class members ( if they aren't removed in the shrinking step ).
  @param keepclassmembernames Preserve the names of the specified class members ( if they aren't removed in the shrinking step ).
  @param keepclasseswithmembernames Preserve the names of the specified classes and class members, if all of the specified class members are present ( after the shrinking step ).
  @param printseeds List classes and class members matched by the various -keep options, to the standard output or to the given file.
  @param dontshrink Don't shrink the input class files.
  @param printusage List dead code of the input class files, to the standard output or to the given file.
  @param whyareyoukeeping Print details on why the given classes and class members are being kept in the shrinking step.
  @param dontoptimize Don't optimize the input class files.
  @param optimizations The optimizations to be enabled and disabled.
  @param optimizationpasses The number of optimization passes to be performed.
  @param assumenosideeffects Assume that the specified methods don't have any side effects, while optimizing.
  @param allowaccessmodification Allow the access modifiers of classes and class members to be modified, while optimizing.
  @param mergeinterfacesaggressively Allow any interfaces to be merged, while optimizing.
  @param dontobfuscate Don't obfuscate the input class files.
  @param printmapping Print the mapping from old names to new names for classes and class members that have been renamed, to the standard output or to the given file.
  @param applymapping Reuse the given mapping, for incremental obfuscation.
  @param obfuscationdictionary Use the words in the given text file as obfuscated field names and method names.
  @param classobfuscationdictionary Use the words in the given text file as obfuscated class names.
  @param packageobfuscationdictionary Use the words in the given text file as obfuscated package names.
  @param overloadaggressively Apply aggressive overloading while obfuscating.
  @param useuniqueclassmembernames Ensure uniform obfuscated class member names for subsequent incremental obfuscation.
  @param dontusemixedcaseclassnames Don't generate mixed-case class names while obfuscating.
  @param keeppackagenames Keep the specified package names from being obfuscated.
  @param flattenpackagehierarchy Repackage all packages that are renamed into the single given parent package.
  @param repackageclasses Repackage all class files that are renamed into the single given package.
  @param keepattributes Preserve the given optional attributes; typically Exceptions, InnerClasses, Signature, Deprecated, SourceFile, SourceDir, LineNumberTable, LocalVariableTable, LocalVariableTypeTable, Synthetic, EnclosingMethod, and *Annotation*.
  @param keepparameternames Keep the parameter names and types of methods that are kept.
  @param renamesourcefileattribute Put the given constant string in the SourceFile attributes.
  @param adaptclassstrings Adapt string constants in the specified classes, based on the obfuscated names of any corresponding classes.
  @param adaptresourcefilenames Rename the specified resource files, based on the obfuscated names of the corresponding class files.
  @param adaptresourcefilecontents Update the contents of the specified resource files, based on the obfuscated names of the processed classes.
  @param dontpreverify Don't preverify the processed class files.
  @param microedition Target the processed class files at Java Micro Edition.
  @param verbose Write out some more information during processing.
  @param dontnote Don't print notes about potential mistakes or omissions in the configuration.
  @param dontwarn Don't warn about unresolved references at all.
  @param ignorewarnings Print warnings about unresolved references, but continue processing anyhow.
  @param printconfiguration Write out the entire configuration in traditional ProGuard style, to the standard output or to the given file.
  @param dump Write out the internal structure of the processed class files, to the standard output or to the given file.
  /* AUTO GENERATED SECTION END: docs */
    */
  case class proguard(
    /* AUTO GENERATED SECTION BEGIN: params */
    include:                              Option[File]                              = None,
    basedirectory:                        Option[File]                              = None,
    injars:                               Option[Seq[File]]                         = None,
    outjars:                              Option[Seq[File]]                         = None,
    libraryjars:                          Option[Seq[File]]                         = None,
    skipnonpubliclibraryclasses:          Boolean                                   = false,
    dontskipnonpubliclibraryclasses:      Boolean                                   = false,
    dontskipnonpubliclibraryclassmembers: Boolean                                   = false,
    keepdirectories:                      Option[Option[String]]                    = None,
    target:                               Option[String]                            = None,
    forceprocessing:                      Boolean                                   = false,
    keep:                                 Option[( Seq[KeepOptionModifier], String )] = None,
    keepclassmembers:                     Option[( Seq[KeepOptionModifier], String )] = None,
    keepclasseswithmembers:               Option[( Seq[KeepOptionModifier], String )] = None,
    keepnames:                            Option[String]                            = None,
    keepclassmembernames:                 Option[String]                            = None,
    keepclasseswithmembernames:           Option[String]                            = None,
    printseeds:                           Option[Option[File]]                      = None,
    dontshrink:                           Boolean                                   = false,
    printusage:                           Option[Option[File]]                      = None,
    whyareyoukeeping:                     Option[String]                            = None,
    dontoptimize:                         Boolean                                   = false,
    optimizations:                        Option[String]                            = None,
    optimizationpasses:                   Option[Int]                               = None,
    assumenosideeffects:                  Option[String]                            = None,
    allowaccessmodification:              Boolean                                   = false,
    mergeinterfacesaggressively:          Boolean                                   = false,
    dontobfuscate:                        Boolean                                   = false,
    printmapping:                         Option[Option[File]]                      = None,
    applymapping:                         Option[File]                              = None,
    obfuscationdictionary:                Option[File]                              = None,
    classobfuscationdictionary:           Option[File]                              = None,
    packageobfuscationdictionary:         Option[File]                              = None,
    overloadaggressively:                 Boolean                                   = false,
    useuniqueclassmembernames:            Boolean                                   = false,
    dontusemixedcaseclassnames:           Boolean                                   = false,
    keeppackagenames:                     Option[Option[String]]                    = None,
    flattenpackagehierarchy:              Option[Option[String]]                    = None,
    repackageclasses:                     Option[Option[String]]                    = None,
    keepattributes:                       Option[Option[String]]                    = None,
    keepparameternames:                   Boolean                                   = false,
    renamesourcefileattribute:            Option[Option[String]]                    = None,
    adaptclassstrings:                    Option[Option[String]]                    = None,
    adaptresourcefilenames:               Option[Option[String]]                    = None,
    adaptresourcefilecontents:            Option[Option[String]]                    = None,
    dontpreverify:                        Boolean                                   = false,
    microedition:                         Boolean                                   = false,
    verbose:                              Boolean                                   = false,
    dontnote:                             Option[Option[String]]                    = None,
    dontwarn:                             Option[Option[String]]                    = None,
    ignorewarnings:                       Boolean                                   = false,
    printconfiguration:                   Option[Option[File]]                      = None,
    dump:                                 Option[Option[File]]                      = None
    /* AUTO GENERATED SECTION END: params */
  ) extends ( () => T ) {
    // type class rendering scala values into string arguments
    private class argsFor[T]( val apply: T => Option[Seq[String]] )
    private object argsFor {
      def apply[T: argsFor]( value: T ) = implicitly[argsFor[T]].apply( value )
      implicit object SeqFile extends argsFor[Seq[File]]( v => Some( Seq( v.map( _.getPath ).mkString( ":" ) ) ) )
      implicit object File    extends argsFor[File]( v => Some( Seq( v.getPath ) ) )
      implicit object String  extends argsFor[String]( v => Some( Seq( v ) ) )
      implicit object Int     extends argsFor[Int]( i => Some( Seq( i.toString ) ) )
      implicit object Boolean
          extends argsFor[Boolean]( {
            case false => None
            case true  => Some( Nil )
          } )
      implicit def Option2[T: argsFor]: argsFor[Option[T]] = new argsFor(
        _.map( implicitly[argsFor[T]].apply( _ ).toSeq.flatten )
      )
      implicit def Option3[T: argsFor]: argsFor[Option[Option[String]]] =
        new argsFor( _.map( _.toSeq ) )
      implicit def SpecWithModifiers: argsFor[( Seq[KeepOptionModifier], String )] =
        new argsFor( {
          case ( modifiers, spec ) =>
            Some( Seq( modifiers.map( _.string ).map( "," ++ _ ).mkString ).filterNot( _ == "" ) :+ spec )
        } )
    }

    // capture string argument values and names
    val args = (
      /* AUTO GENERATED SECTION BEGIN: args */
      argsFor( include ).map( "-include" +: _ )
        ++ argsFor( basedirectory ).map( "-basedirectory" +: _ )
        ++ argsFor( injars ).map( "-injars" +: _ )
        ++ argsFor( outjars ).map( "-outjars" +: _ )
        ++ argsFor( libraryjars ).map( "-libraryjars" +: _ )
        ++ argsFor( skipnonpubliclibraryclasses ).map( "-skipnonpubliclibraryclasses" +: _ )
        ++ argsFor( dontskipnonpubliclibraryclasses ).map( "-dontskipnonpubliclibraryclasses" +: _ )
        ++ argsFor( dontskipnonpubliclibraryclassmembers ).map( "-dontskipnonpubliclibraryclassmembers" +: _ )
        ++ argsFor( keepdirectories ).map( "-keepdirectories" +: _ )
        ++ argsFor( target ).map( "-target" +: _ )
        ++ argsFor( forceprocessing ).map( "-forceprocessing" +: _ )
        ++ argsFor( keep ).map( "-keep" +: _ )
        ++ argsFor( keepclassmembers ).map( "-keepclassmembers" +: _ )
        ++ argsFor( keepclasseswithmembers ).map( "-keepclasseswithmembers" +: _ )
        ++ argsFor( keepnames ).map( "-keepnames" +: _ )
        ++ argsFor( keepclassmembernames ).map( "-keepclassmembernames" +: _ )
        ++ argsFor( keepclasseswithmembernames ).map( "-keepclasseswithmembernames" +: _ )
        ++ argsFor( printseeds ).map( "-printseeds" +: _ )
        ++ argsFor( dontshrink ).map( "-dontshrink" +: _ )
        ++ argsFor( printusage ).map( "-printusage" +: _ )
        ++ argsFor( whyareyoukeeping ).map( "-whyareyoukeeping" +: _ )
        ++ argsFor( dontoptimize ).map( "-dontoptimize" +: _ )
        ++ argsFor( optimizations ).map( "-optimizations" +: _ )
        ++ argsFor( optimizationpasses ).map( "-optimizationpasses" +: _ )
        ++ argsFor( assumenosideeffects ).map( "-assumenosideeffects" +: _ )
        ++ argsFor( allowaccessmodification ).map( "-allowaccessmodification" +: _ )
        ++ argsFor( mergeinterfacesaggressively ).map( "-mergeinterfacesaggressively" +: _ )
        ++ argsFor( dontobfuscate ).map( "-dontobfuscate" +: _ )
        ++ argsFor( printmapping ).map( "-printmapping" +: _ )
        ++ argsFor( applymapping ).map( "-applymapping" +: _ )
        ++ argsFor( obfuscationdictionary ).map( "-obfuscationdictionary" +: _ )
        ++ argsFor( classobfuscationdictionary ).map( "-classobfuscationdictionary" +: _ )
        ++ argsFor( packageobfuscationdictionary ).map( "-packageobfuscationdictionary" +: _ )
        ++ argsFor( overloadaggressively ).map( "-overloadaggressively" +: _ )
        ++ argsFor( useuniqueclassmembernames ).map( "-useuniqueclassmembernames" +: _ )
        ++ argsFor( dontusemixedcaseclassnames ).map( "-dontusemixedcaseclassnames" +: _ )
        ++ argsFor( keeppackagenames ).map( "-keeppackagenames" +: _ )
        ++ argsFor( flattenpackagehierarchy ).map( "-flattenpackagehierarchy" +: _ )
        ++ argsFor( repackageclasses ).map( "-repackageclasses" +: _ )
        ++ argsFor( keepattributes ).map( "-keepattributes" +: _ )
        ++ argsFor( keepparameternames ).map( "-keepparameternames" +: _ )
        ++ argsFor( renamesourcefileattribute ).map( "-renamesourcefileattribute" +: _ )
        ++ argsFor( adaptclassstrings ).map( "-adaptclassstrings" +: _ )
        ++ argsFor( adaptresourcefilenames ).map( "-adaptresourcefilenames" +: _ )
        ++ argsFor( adaptresourcefilecontents ).map( "-adaptresourcefilecontents" +: _ )
        ++ argsFor( dontpreverify ).map( "-dontpreverify" +: _ )
        ++ argsFor( microedition ).map( "-microedition" +: _ )
        ++ argsFor( verbose ).map( "-verbose" +: _ )
        ++ argsFor( dontnote ).map( "-dontnote" +: _ )
        ++ argsFor( dontwarn ).map( "-dontwarn" +: _ )
        ++ argsFor( ignorewarnings ).map( "-ignorewarnings" +: _ )
        ++ argsFor( printconfiguration ).map( "-printconfiguration" +: _ )
        ++ argsFor( dump ).map( "-dump" +: _ )
      /* AUTO GENERATED SECTION END: args */
    ).flatten.toSeq

    def apply: T = {
      outjars.foreach( _.map( _.toPath ).map( deleteIfExists ) )
      val c = main( args )
      if ( c != 0 ) throw new Exception
      T( outjars.toSeq.flatten )
    }
  }
}
