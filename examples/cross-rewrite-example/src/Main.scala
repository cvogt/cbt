object Main {
  def main(args: Array[String]): Unit = {
    //For 2.11.8 rewrite to case match
    Disjunction.intOrString.map(println)
  }
}
