package cbt

import java.net._

case class URLClassLoader(classPath: ClassPath, parent: ClassLoader)
  extends java.net.URLClassLoader(
    classPath.strings.map(
      path => new URL("file:"++path)
    ).toArray,
    parent
  ){
  override def toString = (
    scala.Console.BLUE ++ "cbt.URLClassLoader" ++ scala.Console.RESET
      ++ "(\n  " ++ getURLs.map(_.toString).sorted.mkString(",\n  ")
      ++ (
        if(getParent() != ClassLoader.getSystemClassLoader())
          ",\n" ++ getParent().toString.split("\n").map("  "++_).mkString("\n")
        else ""
      )
      ++ "\n)"
  )
}
