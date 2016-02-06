package cbt
import java.time._
// We can replace this with something more sophisticated eventually
case class Logger(enabledLoggers: Set[String]){
  val start = LocalTime.now()
  //System.err.println("Created Logger("+enabledLoggers+")")
  def this(enabledLoggers: Option[String]) = this( enabledLoggers.toVector.flatMap( _.split(",") ).toSet )
  def log(name: String, msg: => String) = {
    val timeTaken = (Duration.between(start, LocalTime.now()).toMillis.toDouble / 1000).toString
    System.err.println( s"[${" "*(6-timeTaken.size)}$timeTaken]["+name+"] " + msg )
  }
 
  def showInvocation(method: String, args: Any) = method + "( " + args + " )"

  final def stage1(msg: => String) = logGuarded(names.stage1, msg)
  final def stage2(msg: => String) = logGuarded(names.stage2, msg)
  final def loop(msg: => String) = logGuarded(names.loop, msg)
  final def task(msg: => String) = logGuarded(names.task, msg)
  final def composition(msg: => String) = logGuarded(names.composition, msg)
  final def resolver(msg: => String) = logGuarded(names.resolver, msg)
  final def lib(msg: => String) = logGuarded(names.lib, msg)

  private object names{
    val stage1 = "stage1"
    val stage2 = "stage2"
    val loop = "loop"
    val task = "task"
    val resolver = "resolver"
    val composition = "composition"
    val lib = "lib"
  }

  private def logGuarded(name: String, msg: => String) = {
    if(
      (enabledLoggers contains name)
      || (enabledLoggers contains "all")
    ){
      log(name, msg)
    }
  }
}