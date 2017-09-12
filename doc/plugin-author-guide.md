## How to write an idiomatic CBT plugin?

Write a small library that could be fully used outside of CBT's
build classes and handles all the use cases you need. For example

```
object MyLibrary{
 def doSomething( ... ) = // do something here
}
```

Publish it as a library and you might be done right here.

If your library requires configuration information commonly found
in your build, like the sourceFiles, groupId, scalaVersion or else,
consider offering a mixin trait, that pre-configures your library
for user convenience. (Consider publishing them separately if that
allows people to use your library outside of CBT with fewer
dependencies.) Here is an example of a library with an
accompanying mixin trait configuring the library for CBT.

```
package my.library
object MyLibrary{
  case class DoSomething( scalaVersion: String, ... ){
    case class config( targetFile: File, affectBehavior: Boolean = true, ... ){
      def apply = // really do something here
    }
  }
}

package my.plugin
trait MyLibrary extends BaseBuild{
  def doSomething = MyLibrary.DoSomething(scalaVersion, ...).config( scalaTarget / "my.file" )
}
```

* Note: Do not override any common method like `compile` or `test` in a public plugin. *
* Instead document recommendations where users should hook in your custom methods. *
* This will help users understand their own builds. *

See how we only define a single `def doSomething` in the MyLibrary
trait? We did not define things like `def doSomethingTargetFile`.
Instead we have a case class defined in the library which a user
can .copy as needed to customize configuration. A user build could
look like this:

```
class Build(val context: Context) extends MyLibrary{
  override def doSomething = super.doSomething.copy(
    affectBehavior = false
  )
}
```

As you can see a user can use .copy to override default behavior
of your library.

This nesting allows us to keep the global namespace small, which
helps us lower the risk of global name clashes between different
libraries. It also makes it clearer that `affectBehavior` is
something specific to `MyLibary` which should help making builds
easier to understand. Further this nesting means we don't need to
encode namespaces in the names themselves, but use Scala's language
features for that, which allows us to keep names nice and concise.

You might wonder why there is a case class `DoSomething` rather
than `scalaVersion` just being another parameter in case class
`config`. Nesting case classes like this is a pattern that given
the way we use them allows us to make it slightly harder to
modify some parameters (the ones on the outer case class) than
others (the ones on the inner case class). Why? Some are likely
to need user customization, others are likely to break stuff if
they are touched. Example: The scalaVersion is probably something
you want to configure once consistently across your entire build.
Otherwise you might end up accidentally packaging scala-2.11
compiled class files as a jar with a `_2.10` artifact id.
Changing which targetFile `doSomething` writes to however is
something you should be able to safely change. Since we defined
`doSomething` as
`def doSomething = MyLibrary.DoSomething(...).config( ... )`
overriding behavior in user code with .copy only affects
the inner case class because super.doSomething is an instance
of that:
```
  override def doSomething = super.doSomething.copy(
    affectBehavior = false
  )
```
Overriding things in class `DoSomething` is possible by creating
an entire new instance of the outer one, but slightly harder
preventing users from accidentally doing the wrong thing.

Obviously this decisions what's dangerous to override and what
is not can be a judgment call and not 100% clear.

A few more conventions for more uniform plugin designs:
If you only have one outer case class in yur plugin `object`,
call the case class `apply` instead of `DoSomething`. If you
need multiple (because you basically have several commands,
each with their own private configuration), give it a name
representing the operation, e.g. `compile` or `doc`. If there
is only one inner case class inside of anothe case class,
call it `config`, give it a name representing the operation,
e.g. `compile` or `doc`.

### Finalizing your plugin

Add your plugin to stage2/plugins.scala

Create a test project, and then test it in test/test.scala
