object Main extends Foo("Hello Dotty - trait parameters, yay"){
  def main(args: Array[String]) = {  
    println(hello)
  }
}

trait Foo(val hello: String)
