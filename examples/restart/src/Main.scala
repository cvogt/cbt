object Main extends App {
  while(true){
    Thread.sleep(1000)
    println( "process " + cbt.process.currentProcessId + " is still running" )
  }
}
