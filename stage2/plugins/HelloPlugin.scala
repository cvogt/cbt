package cbt

import cbt.{BaseBuild, Context}

class HelloPlugin(val context: Context) extends BaseBuild {
  override def compile: Option[Long] = {
    println("Hello")
    super.compile
  }
}
