### Uber-jar plugin example

This example shows how to build uber jar(aka fat jar) with `UberJar` plugin.

In order to create uber jar: execute `cbt uberJar`. Produced jar will be in target folder.

By default, jar name is your `cbt projectName`, you can provide other name via overriding `uberJarName` task.

By default, main class is `Main`. You can provide custom main class via overriding `uberJarMainClass` task.

To run your main class you can execute `java -jar your-jar-name.jar`.

You can also run scala REPL with your jar classpath and classes with this command: `scala -cp your-jar-name.jar`.

In scala REPL you will have access to all your project classes and dependencies.

```
scala -cp uber-jar-example-0.0.1.jar
Welcome to Scala 2.11.8 (Java HotSpot(TM) 64-Bit Server VM, Java 1.8.0_72).
Type in expressions for evaluation. Or try :help.

scala> import com.github.someguy.ImportantLib
import com.github.someguy.ImportantLib

scala> ImportantLib.add(1,2)
res0: Int = 3

scala> ImportantLib.currentDirectory
Current directory is: /Users/rockjam/projects/cbt/examples/uber-jar-example/target

scala> Main.main(Array.empty)
fooo
Current directory is: /Users/rockjam/projects/cbt/examples/uber-jar-example/target
not empty list

scala> import shapeless._
import shapeless._

scala> 1 :: "String" :: 3 :: HNil
res3: shapeless.::[Int,shapeless.::[String,shapeless.::[Int,shapeless.HNil]]] = 1 :: String :: 3 :: HNil
```
