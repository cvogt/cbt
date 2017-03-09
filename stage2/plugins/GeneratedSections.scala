package cbt
import java.nio.file.Files._
object replaceSections{
  def apply(
    subject: String,
    sections: Seq[(String, String)],
    generatedSectionBeginMarker: String => String = name => s"AUTO GENERATED SECTION BEGIN: $name ",
    generatedSectionEndMarker: String => String = name => s"AUTO GENERATED SECTION END: $name "
  ): String = {
    assert(
      generatedSectionBeginMarker("foo").endsWith(" "),
      "generatedSectionStartMarker needs to end with a space character"
    )
    assert(
      generatedSectionEndMarker("foo").endsWith(" "),
      "generatedSectionEndMarker needs to end with a space character"
    )
    sections.headOption.map{
      case (name, replacement) =>
        replaceSections(
          s"(?s)(\n[^\n]*AUTO GENERATED SECTION BEGIN: $name [^\n]*\n).*(\n[^\n]*AUTO GENERATED SECTION END: $name [^\n]*\n)"
            .r.replaceAllIn( subject, m => m.group(1) ++ replacement ++ m.group(2) ),
          sections.tail
        )
    }.getOrElse(subject)
  }
}
