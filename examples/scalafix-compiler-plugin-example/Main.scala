object Main{
  lazy val x = 1 + 1
  
  implicit def toString(i :Int) = i.toString

  def main( args: Array[String] ){
    println("Hello world!")
  }
}
