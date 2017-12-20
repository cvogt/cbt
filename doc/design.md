# Design

This chapter explains some of CBT's less obvious design decisions and
the reasons behind them. Hopefully this answers some of the questions
often unanswered when trying to understand a new tool.


## Why does CBT use inheritance?

First of all CBT uses classes, because it needs some place to put tasks and
name them. Methods in classes are one way to do it. Methods calling each
other allows to effectively build a graph of dependent tasks.

Inheritance on top of that allows patching this graph to insert additional
steps, e.g. formatting the code before compiling it. Patching of the task
graph is what you do with sbt andso with CBT via inheritance/overrides.

This was also discussed in gitter here: https://gitter.im/cvogt/cbt?at=58a95663de50490822e869e5

Taking a graph and continuously patching it can be confusing, which is
why inheritance is confusing. You can even build non-terminating call
cycles. CBT's logic is not coupled to the inheritance layer. You can
write CBT builds without inheritance. They require a bit more code, but
may end up easier to maintain and understand.

## Task composition and aborting failed tasks and dependents

In CBT build tasks are methods. Tasks can depend on each
other by invoking each other. E.g. `package` is a method that invokes `compile`.
Build tasks can fail. By convention, if a task fails it is expected to throw
an Exception in order to also abort the execution of dependent tasks. When
`compile` finds compile errors it throws an exception and thereby also
drops out of the `package` execution pth. CBT catches and handles the
exception and returns control back to the user.

This design was chosen for simplicity and because build code lives in a
world with exceptions anyways as a lot of it is directly or indirectly
using java libraries. We might reconsider this design at some point. Or not.
The hope is that Exceptions are without major drawbacks and more
approachable to newcomers than monadic error handling, which would require
wrapper types and value composition via .map and for-expressions. Not using
a Monad however also means that CBT cannot reason about task composition
automatically, which means parallel task execution is not automatic, but
manual and opt-in where wanted.

## Why do CBT plugins use case classes where methods seem simpler?

In CBT plugins you may see

```
trait SomePlugin{
  def doSomething = SomePluginLib(...context).doSomething( a, b, ... )
}
case class SomePluginLib(...context...)(implicit ...more context...){
  case class doSomething(a: A, b: B, ...){
    def apply = {
      // logic
    }
  }
}
```

Why this? This seems much simpler:

```
trait SomePlugin{
  def doSomething = SomePluginLib.doSomething( a, b, ... )(...context)
}
object SomePluginLib
  def doSomething(a: A, b: B, ...)(...context...)(implicit ...more context...) = {
    // logic
  }
}
```

The reason is that the former allows easy patching while
the second does not. Let's pretend your build is this:
```
class Build(val context: Context) extends SomePlugin{
  override def doSomething = super.doSomething.copy( b = someOtherB )
}
```
Such a simple replacement of `b` while keeping all other arguments would
not be easily possible if doSomething was a def not a case class.

## Why do the libraries have ops packages and Module traits?

Java's and Scala's package system does allow importing things,
but not exporting things. Everything has to be imported
explicitly anywhere it is supposed to be used. It's just a
package system, not a module system. This leads to a lot of
import boiler plate. CBT tries to minimize the imports
necessary for it's use however. So how to we do this while
at the same time allowing modularization? In particular, how
do we do this with stand-alone methods and implicit classes
that have to be in an object, e.g. the package object?

Scala's traits can be used as a module system that supports exports.
This means we can take several modules (traits) and merge them into
something that exports everything defined in any of them. Basically
inheriting a trait means importing names into the scope of the
inheriting class and exporting those names to the class's users.

CBT's libraries define Module traits, which their package objects inherit.
This makes it easy to use the libraries by itself. CBT's core however
also inherits all of the library Module traits in it's package object,
meaning that by a simple `import cbt._` you get everything from all
libraries. This solves the import boiler plate.

For implicit classes it is a little bit trickier as those should
extend AnyVal for performance reasons, but that's only allowed in
an object, not a trait. So what we do instead is put Universal traits
(see http://docs.scala-lang.org/overviews/core/value-classes.html)
containing all the logic into a helper package `ops`. The package
objects that want to offer the implicit classes now define them
extending the Universal traits. This means a little boiler plate
where the package object is define, but also solves the import
boiler plate everywhere else.

## What is newBuild and why do we need it?

Methods in a class can call each other and thereby effectively form a graph.
Subclasses can patch this graph. Sometimes you want to create several
different variants of a graph. Let's say one building artifacts with a
-SNAPSHOT version and another one building a stable release. Or one with
scalaVersion 2.10 and one with scalaVersion 2.11. Or one optmizing your
code and one not optimizing it. You can subclass your build once for
each alternative. That's fine if you know which class to subclass, but
plugins for your build do not know what your concrete build class is
and cannot produce subclasses for it. Example

```
class Build(val context: Context){
  // this works fine
  def with210 = new Build(context){
    override def scalaVersion = "2.10"
  }
  def with211 = new Build(context){
    override def scalaVersion = "2.11"
  }
}
```
Imagine however
```
// defined by you:
class Build(val context: Context) extends CrossVersionPlugin
// defined by the plugin author:
trait CrossVersionPlugin{
  // this does not compile, Build does not exists when the plugin is compiled
  def with210 = new Build(context){
    override def scalaVersion = "2.10"
  }
  def with211 = new Build(context){
    override def scalaVersion = "2.11"
  }
}
```
Luckily CBT can cheat because it has a compiler available.
```
// defined by the plugin author:
trait CrossVersionPlugin{
  // method `newBuild` generates a new subclass of Build at runtime and creates an instance.
  def with210 = newBuild{"""
    override def scalaVersion = "2.10"
  """}
  def with211 = newBuild{"""
    override def scalaVersion = "2.11"
  """}
}
```

Problem solved. In fact this allows for a very, very flexible way of
creating differents variants of your build.

