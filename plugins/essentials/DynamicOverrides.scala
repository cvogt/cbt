package cbt
import cbt.eval.Eval
trait DynamicOverrides extends BaseBuild{
  private val twitterEval = cached("eval"){
    new Eval{
      override lazy val impliedClassPath: List[String] = context.parentBuild.get.classpath.strings.toList//new ScalaCompilerDependency( context.cbtHasChanged, context.paths.mavenCache, scalaVersion ).classpath.strings.toList
      override def classLoader = DynamicOverrides.this.getClass.getClassLoader
    }
  }

  protected [cbt] def overrides: String = ""

  // TODO: add support for Build inner classes
  def newBuild[T <: DynamicOverrides:scala.reflect.ClassTag]: DynamicOverrides with T = newBuild[T](context)("")
  def newBuild[T <: DynamicOverrides:scala.reflect.ClassTag](body: String): DynamicOverrides with T = newBuild[T](context)(body)
  def newBuild[T <: DynamicOverrides:scala.reflect.ClassTag](context: Context)(body: String): DynamicOverrides with T = {
    val mixinClass = scala.reflect.classTag[T].runtimeClass
    assert(mixinClass.getTypeParameters.size == 0)
    val mixin = if(
      mixinClass == classOf[Nothing]
      || mixinClass.getSimpleName == "Build"
    ) "" else " with "+mixinClass.getName
    import scala.collection.JavaConverters._
    val parent = Option(
      if(this.getClass.getName.startsWith("Evaluator__"))
        this.getClass.getName.dropWhile(_ != '$').drop(1).stripSuffix("$1")
      else
        this.getClass.getName
    ).getOrElse(
      throw new Exception( "You cannot have more than one newBuild call on the Stack right now." )
    )
    val overrides = "" // currently disables, but can be used to force overrides everywhere
    val name = if(mixin == "" && overrides == "" && body == ""){
      "Build"
    } else if(overrides == ""){
      val name = "DynamicBuild" + System.currentTimeMillis
      val code = s"""
        class $name(context: _root_.cbt.Context)
          extends $parent(context)$mixin{
            $body
        }
      """
      logger.dynamic("Dynamically generated code:\n" ++ code)
      twitterEval.compile(code)
      name
    } else {
      val name = "DynamicBuild" + System.currentTimeMillis
      val code = s"""
        class $name(context: _root_.cbt.Context)
          extends $parent(context)$mixin{
            $body
        }
        class ${name}Overrides(context: _root_.cbt.Context)
          extends $name(context){
            $overrides
        }
      """
      logger.dynamic("Dynamically generated code:\n" ++ code)
      twitterEval.compile(code)
      name+"Overrides"
    }

    val createBuild = twitterEval.apply[Context => T](s"new $name(_: _root_.cbt.Context)",false)
    createBuild( context ).asInstanceOf[DynamicOverrides with T]
  }
}
