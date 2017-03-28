import cbt._

import scala.meta._
import scalafix.rewrite._
import scalafix.util._
import scalafix.util.TreePatch._
import scalafix.util.TokenPatch._

class Build(val context: Context) extends BaseBuild{
  override def compile = {
    new BasicBuild(context) with Scalafix{
      override def scalafix = super.scalafix.copy(
        patches =
          Seq(
            AddGlobalImport(
              importer"scala.collection.immutable"
            )
          ),
        rewrites = Seq(
          ProcedureSyntax,
          ExplicitImplicit,
          VolatileLazyVal
        )
      )
    }.scalafix.apply
    super.compile // <- as scalafix trigger compile already before re-writing, this will just return the cached compile result before rewriting
  }
}
