package com.github.someguy

import java.nio.file.Paths

object ImportantLib {
  def add(a: Int, b: Int): Int = a + b
  def currentDirectory() = {
    println(fansi.Color.Green(s"Current directory is: ${Paths.get("").toAbsolutePath}"))
  }

}
