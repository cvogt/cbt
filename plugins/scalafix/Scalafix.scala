package cbt

import java.io.File

import scala.meta._
import scala.meta.semantic.v1._
import scala.meta.{ Symbol => _, _ }
import scalafix._, rewrite._, config._, util._
import org.scalameta.{logger => scalametaLogger}

import cbt._

trait Scalafix extends Scalameta{
  def scalafix = Scalafix.apply( lib ).config(
    classpath,
    sourceFiles zip sourceFiles
  )
}

object Scalafix{
  case class apply( lib: Lib ){
    case class config(
      classpath: ClassPath,
      files: Seq[(File,File)],
      patches: Seq[Patch] = Seq(),
      rewrites: Seq[ Rewrite[ScalafixMirror] ] = Seq(),
      allowEmpty: Boolean = false
    ){
      def mirror =
        Mirror(
          classpath.string,
          files.map(_._1).mkString(File.pathSeparator)
        )

      def context(file: File): ( RewriteCtx[Mirror], RewriteCtx[ScalafixMirror] ) = (
        scalafix.rewrite.RewriteCtx(
          mirror.dialect(file).parse[Source].get, ScalafixConfig(), mirror
        ),
        scalafix.rewrite.RewriteCtx(
          mirror.dialect(file).parse[Source].get, ScalafixConfig(), ScalafixMirror.fromMirror( mirror )
        )
      )

      def apply: Unit = {
        require(
          allowEmpty || rewrites.nonEmpty || patches.nonEmpty,
          "You need to provide some rewrites via: `override def scalafix = super.scalafix.copy( rewrites = Seq(...) )`"
        )
        files.foreach{ case (from, to) =>
          implicit val ( ctx, ctx2 ) = context(from)
          lib.writeIfChanged(
            to,
            Patch(
              (
                patches
                ++ rewrites.flatMap(
                  _.rewrite( ctx2 ).to
                )
              ).to
            )
          )
        }
      }
    }
  }
}
