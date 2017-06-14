package kindprojector_example

object Main {

  def parametric[F[_]]: Unit = ()

  def main(args: Array[String]): Unit = {
    parametric[Either[String, ?]]
  }
}
