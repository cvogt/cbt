import cbt._

import org.wartremover.warts.{ Null, Var }
import org.wartremover.WartTraverser

class Build(val context: Context) extends BuildBuild with WartRemover {

  override def wartremoverErrors: Seq[WartTraverser] = Seq(Var, Null)
}
