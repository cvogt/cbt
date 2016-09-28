object Main extends Foo("Hello Dotty - trait parameters, yay"){
  def main(args: Array[String]) = {  
    println(hello)

    // Sanity check the classpath: this won't run if the dotty jar is not present.
    val x: Int => Int = z => z
    x(1)
  }
}

trait Foo(val hello: String)
