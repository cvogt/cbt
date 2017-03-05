package cbt
import java.nio.file.Files._
trait GeneratedSections extends BaseBuild{
  def generatedSectionStartMarker( name: String ) = s"AUTO GENERATED SECTION BEGIN: $name "
  def generatedSectionEndMarker( name: String ) = s"AUTO GENERATED SECTION END: $name "
  assert(
    generatedSectionStartMarker("foo").endsWith(" "),
    "generatedSectionStartMarker needs to end with a space character"
  )
  assert(
    generatedSectionEndMarker("foo").endsWith(" "),
    "generatedSectionEndMarker needs to end with a space character"
  )

  def replacements: Seq[(String, String)]

  def generate = {
    def replaceSections(subject: String, sections: Seq[(String, String)]): String = {
      sections.headOption.map{
        case (name, replacement) =>
          replaceSections(
            s"(?s)(\n[^\n]*AUTO GENERATED SECTION BEGIN: $name [^\n]*\n).*(\n[^\n]*AUTO GENERATED SECTION END: $name [^\n]*\n)"
              .r.replaceAllIn( subject, m => m.group(1) ++ replacement ++ m.group(2) ),
            sections.tail
          )
      }.getOrElse(subject)
    }

    val updated = sourceFiles.flatMap{ file =>
      val template = file.readAsString
      val replaced = replaceSections( template, replacements )
      if( template != replaced ) {
        write( file.toPath, replaced.getBytes )
        Some(file)
      } else None
    }

    logger.log("generated-sections","Updated:" + updated.map(_ ++ "\n").mkString)
  }
}
