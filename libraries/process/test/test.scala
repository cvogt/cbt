object Tests{
  def main(args: Array[String]): Unit = {
    val pb = new ProcessBuilder("cat")
    val p = pb.start
    cbt.process.getProcessId( p ) // checks that it actually gets a process id
  }
}
