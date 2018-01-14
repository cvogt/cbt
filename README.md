[![Join us on gitter](http://badges.gitter.im/cvogt/cbt.png)](https://gitter.im/cvogt/cbt)

(For a tutorial scroll down.)

Chris' Build Tool (CBT) for Scala
============================================

Easy to learn and master, lightning fast and backed by a thriving community of enthusiasts and contributors. For talks, development roadmap, projects using cbt, etc see the wiki.

What is CBT?
----------------------------------------------
CBT is a build tool meaning it helps orchestrating compilation, code and
documentation generation, packaging, deployment and custom tooling for
your project. It mainly targets Scala projects but is not exclusive to them.

CBT builds are full programs written using vanilla Scala code.
Familiar concepts make you feel right at home - build files are classes,
tasks are defs, you customize using method overrides. You already know these
things and everything behaves as expected. That way implementing any
build related requirement becomes as easy as writing any other Scala code.

CBT is simple in the sense that it uses very few concepts.
A single build uses classes, defs and inheritance.
Builds and binary dependencies can be composed to model 
modules depending on each other.

CBT believes good integration with existing tools to be very
helpful. In that spirit CBT aims for excellent integration with
the command line and your shell.

CBT considers source files to be an excellent way to distribute code
and has first class support for source and git dependencies.

How is CBT different from other build tools?
----------------------------------------------
Not all build tools allow you to write builds in a full programming language.
CBT is based on the assumption that builds are complex enough problems to
warrant this and abstraction and re-use is better handled through libraries
rather than some restricted, declarative DSL. CBT shares this philosophy with sbt.
(This also means that integration with external tools such as an IDE better happens
programmatically through an api rather than a static data representation such as xml.)

Like sbt, CBT chooses Scala as its language of choice, trying to appeal to
Scala programmers allowing them to re-use their knowledge and the safety of the language.

Unlike sbt 0.11 and later, CBT maps task execution to JVM method invocations.
sbt implements its own self-contained task graph model and interpreter.
This allows sbt to have its model exactly fit the requirements.
CBT instead uses existing JVM concepts for the solution and adds
custom concepts only when necessary. CBT assumes this to lead to better
ease of use due to familarity and better integration with existing tools
such as interactive debuggers or stack traces because CBT's task call stack
IS the JVM call stack.

sbt 0.7 shared this design decision as many may have forgotten.
However, CBT is still quite a bit simpler than even sbt 0.7 as
CBT gets away with fewer concepts. sbt 0.7 had notions of
main vs test sources, multi-project builds, task-dependencies
which weren't invocations and other concepts. CBT maps all of
these to def invocations and build composition instead.

System requirements
-------------------

CBT is best tested under OSX. People are also using it also under
Ubuntu and Windows via cygwin. It should be easy to port CBT to
other systems or drop the cygwin requirement. You will only
have to touch the launcher bash or .bat scripts.
Please contribute back if you fixed something :).

You currently need javac and realpath or gcc installed.
nailgun is optional for speedup. gpg is required only for publishing maven artifacts.

Features
--------

CBT supports the basic needs for Scala builds right now:
Compiling, running, testing, packaging, publishing local and to sonatype,
scaladoc, maven dependencies, source dependencies (e.g. for modularized projects),
triggering tasks on file changes, cross-compilation, reproducible builds.

There is also a growing number of plugins in `plugins/` and `stage2/plugins/`,
but some things you'd like may still be missing. Consider writing
a plugin in that case. It's super easy, just a trait. Share it :).


Tutorial
--------

This section explains how to get started with cbt step-by-step.
There are also example projects with build files in `examples/` and `test/`.

### Installation

If you haven't cloned cbt yet, clone it now. Cloning is how you install cbt.
We know that's a bit unusual, but roll with it, there are good reasons :).
Open a shell, cd to the directory where you want to install cbt and execute:

```
$ git clone https://github.com/cvogt/cbt.git
```

There are a bash script `cbt` and a `cbt.bat` in the checkout directory.
Add one to your `$PATH`, e.g. symlink it from `~/bin/cbt`.

Check that it works by calling `cbt`. You should see CBT compiling itself
and showing a list of built-in tasks.

Great, you're all set up. Now, let's use cbt for a new example project.
Follow the below steps. (There is also an experimental GUI described
later to create a project, but going through the steps this time will help
you understand what exactly is going on.)

### Creating your first project

Create a new directory and cd into it. E.g. `my-project`.

```
$ mkdir my-project
$ cd my-project
```

Let's create a tiny sample app. CBT can generate it for you. Just run:

```
$ cbt tools createMain
```

### Running your code

Now there should be a file `Main.scala`, which prints `Hello World` when run.
So let's run it:

```
$ cbt run
```

You should see how CBT first compiles your project, then runs it and prints
`Hello World`. CBT created the file `Main.scala` top-level in your directory.
You can alternatively place `.scala` or `.java` files in `src/`
or any of its subdirectories.

### Creating a build file

Without a build file, CBT just uses some default build settings.
Let's make the build more concrete by creating a build file.

CBT can help you with that. Execute:

```
$ cbt tools createBuild
```

Now there should be a file `build/build.scala` with a sample `Build` class.

Btw., a build file can have its own build and so on recursively like in sbt.
When you create a file `build/build/build.scala` and change `Build` class in there
to extend `BuildBuild`, it will be used to build your `build/build.scala`. You can
add built-time dependencies like plugins this way.

### Adding dependencies

In the generated `build/build.scala` there are
several examples for dependencies. We recommend using the constructor syntax
`ScalaDependency` (for automatically adding the scala version to the artifact id)
or `MavenDependency` (for leaving the artifact id as is). The sbt-style `%`-DSL
syntax is also supported for copy-and-paste convenience, but discouraged.

Alright, let's enable the `override def dependencies`. Make sure to include
`super.dependencies`, which currently only includes the Scala standard library.
Add a dependency of your choice, start using it from `Main.scala` and `cbt run` again.

As you can see CBT makes choice of the maven repository explicit. It does so for clarity.

### Calling other tasks

Tasks are just defs. You can call any public zero-arguments method of your
`Build` class or its parents straight from the command line. To see how it works
let's call the compile task.

```
$ cbt compile
```

### Creating custom tasks

In order to create a custom task, simply add a new def to your Build class, e.g.

```
class Build...{
  ...
  def foo = "asdf"
}
```

Now call the def from the command line:

```
$ cbt foo
```

As you can see it prints `asdf`. Adding tasks is that easy.

### Triggering tasks on file-changes

When you call a task, you can prefix it with `loop`. You need to
have fswatch install (e.g. via `brew install fswatch`).
CBT then watches the source files, the build files and even CBT's own
source code and re-runs the task when anything changes. If necessary,
this forces CBT to re-build itself, the project's dependencies and the project itself.

Let's try it. Let's loop the run task. Call this from the shell:

```
$ cbt loop run
```

Now change `Main.scala` and see how cbt picks it up and re-runs it.
CBT is fast. It may already be done re-compiling and re-running before
you managed to change windows back from your editor to the shell.

Try changing the build file and see how CBT reacts to it as well.

To also clear the screen on each run use:
```
$ cbt loop clear run
```

To call and restart the main method on file change (like sbt-revolver)
```
$ cbt direct loop restart
```

### Adding tests

The simplest way to add tests is putting a few assertions into the previously
created Main.scala and be done with it. Alternatively you can add a test
framework plugin to your build file to use something more sophisticated.

This however means that the class files of your tests will be included in the
jar should you create one. If that's fine, you are done :). If it is not you
need to create another project, which depends on your previous project. This
project will be packaged separately or you can disable packaging there. Let's create
such a project now.

Your project containing tests can be anywhere but a recommended location is a
sub-folder called `test/` in your main project. Let's create it and create a
Main class and build file:

```
$ mkdir test
$ cd test
$ rm ../Main.scala
$ cbt tools createMain
$ cbt tools createBuild
```

We also deleted the main projects `Main.scala`, because now that we created a new one
we would have two classes with the same name on the classpath which can be very confusing.

Now that we have a Main file in our test project, we can add some assertions to it.
In order for them to see the main projects code, we still need to do one more thing - 
add a `DirectoryDependency` to your test project's build file. There is a similar example
in the generated `build.scala`. What you need is this:

```
override def dependencies = super.dependencies ++ Seq(
  DirectoryDependency( projectDirectory ++ "/.." )
)
```

This successfully makes your test project's code see the main projects code.
Add some class to your main project, e.g. `case class Foo(i: Int = 5)`. Now
put an assertion into the Main class of your test project, e.g.
`assert(Foo().i == 5)` and hit `cbt run` inside your test project.

Make sure you deleted your main projects class Main when running your tests.

Congratulations, you successfully created a dependent project and ran your tests.

### Dynamic overrides and eval

By making your Build extend trait `CommandLineOverrides` you get access to the `eval` and `with` commands.

`eval` allows you to evaluate scala expressions in the scope of your build and show the result.
You can use this to inspect your build.

```
$ cbt eval '1 + 1'
2

$ cbt eval scalaVersion
2.11.8

$ cbt eval 'sources.map(_.string).mkString(":")'
/a/b/c:/d/e/f
```

`with` allows you to inject code into your build (or rather a dynamically generated subclass).
Follow the code with another task name and arguments if needed to run tasks of your modified build.
You can use this to override build settings dynamically.

```
$ cbt with 'def hello = "Hello"' hello
Hello

$ cbt with 'def hello = "Hello"; def world = "World"; def helloWorld = hello ++ " " ++ world' helloWorld
Hello World

$ cbt with 'def version = super.version ++ "-SNAPSHOT"' package
/a/b/c/e-SNAPSHOT.jar
```

### Multi-projects Builds

A single build only handles a single project in CBT. So there isn't exactly
such a thing as a Multi-project Build. Instead you can simply write multiple
projects that depend on each other. We have already done that with tests above,
but you can do the exact same thing to modularize your project into multiple ones.

### Reproducible builds

To achieve reproducible builds, you'll need to tie your build files to a particular
CBT-version. It doesn't matter what version of CBT you are actually running,
as long as the `BuildInterface` is compatible (which should be true for a large number
of versions and we may find a better solution long term. If you see a compile error 
during compilation of CBT itself that some method in BuildInterface was not
implemented or incorrectly implemented, you may be running an incompatible CBT
version. We'll try to fix that later, but for now you might have to checkout
the required hash of CBT by hand.).

When you specify a particular version, CBT will use that one instead of the installed one.

You can specify one by adding one line right before `class Build`. It looks like this:

```
// cbt:https://github.com/cvogt/cbt.git#f11b8318b85f16843d8cfa0743f64c1576614ad6
class Build...
```

The URL points to any git repository containing one of CBT's forks. You currently
have to use a stable reference - i.e. a hash or tag. (Checkouts are currently not
updated. If you refer to a branch or tag which is moved on the remote, CBT
will not realize that and keep using the old version).

### Using CBT like a boss

Do you own your Build Tool or does your Build Tool own you? CBT makes it easy for YOU
to be in control. We try to work on solid documentation, but even good
documentation never tells the whole truth. Documentation can tell how to use
something and why things are happening, but only the code can tell all the
details of what exactly is happening. Reading the code can be intimidating for
many Scala libraries, but not so with CBT. The source code is easy to read
to the point that even Scala beginners will be able to understand it. So
don't be afraid to actually look under the hood and check out what's happening.

And guess what, you already have the source code on your disk, because
you installed CBT by cloning its git repository. You can even debug CBT and
your build files in an interactive debugger like IntelliJ after some minor setup.

Finally, you can easily change CBT's code. Then CBT re-builds itself when you try
to use it the next time. This means any changes you make are instantly reflected.
This and the simple code make it super easy to fix bugs or add features yourself
and feed them back into main line CBT.

When debugging things, it can help to enable CBT's debug logging by passing
`-Dlog=all` to CBT (or a logger name instead of `all`).

Other design decisions
--------------------

CBT tries to couple its code very loosely. OO is used for configuration in build files.
Interesting logic is in simple supporting library classes/objects, which can be used
independently. You could even build a different configuration api than OO on top of them.


Known limitations
--------------------------
- currently CBT supports no generic task scoping. A solution is known, but not implemented.
  For now manually create intermediate tasks which serve as scoping for
  known situations and encode the scope in the name, e.g. fastOptScalaJsOptions
- currently CBT supports no dynamic overrides of tasks. A solution is known, but not implemented.
  scalaVersion and version are passed through the context instead for dynamic overrides.
- there is currently no built-in support for resources being added to a jar.
  Should be simple to add, consider a PR
- test framework support is currently a bit spotty but actively being worked on
- concurrent task and build execution is currently disabled
- CBT uses its own custom built maven resolver, which is really fast,
  but likely does not work in some edge cases. Those may or may not be easy to fix.
  We should add optional coursier integration back for a more complete solution.

Known bugs
-------------
- currently there is a bug in CBT where dependent builds may miss changes in the things
  they depend on. Deleting all target directories and starting from scratch helps.
- There are some yet unknown bugs which can be solved by killing the nailgun background process
  and/or re-running the desired cbt task multiple times until it succeeds.
- if you ever see no output from a command but expect some, make sure you are in the right directory
  and try to see if any of the above recommendations help


IDE Support
-------------------

### IntelliJ

CBT support has recently been added to the Scala Plugin for IntelliJ. See details [in the docs](doc/intellij-gsoc.md), 
and [view/submit](https://youtrack.jetbrains.com/issues?q=CBT) issues on the JetBrains issue tracker.

Shell completions
-------------------

### Bash completions
To auto-complete cbt task names in bash do this:

```
mkdir ~/.bash_completion.d/
cp shell-integration/cbt-completions.bash ~/.bash_completion.d/
```

Add this to your .bashrc
```
for f in ~/.bash_completion.d/*; do
    source $f
done
```

### Fish shell completions

copy this line into your fish configuration, on OSX: `/.config/fish/config.fish`

```
complete -c cbt -a '(cbt taskNames)'
```

### Zsh completions

##### Manual installation
Add the following to your `.zshrc`
```
source /path/to/cbt/shell-integration/cbt-completions.zsh
```
##### oh-my-zsh
If using oh-my-zsh, you can install it as a plugin:
```
mkdir ~/.oh-my-zsh/custom/plugins/cbt
cp shell-integration/cbt-completions.zsh ~/.oh-my-zsh/custom/plugins/cbt/cbt.plugin.zsh
```
Then enable it in your `.zshrc`:
```
plugins=( ... cbt)
```
Experimental GUI
--------------------

### Creating a project via the GUI

`cd` into the directory inside of which you want to create a new project directory and run `cbt tools gui`.

E.g.

```
$ cd ~/my-projects
$ cbt tools gui
```

This should start UI server at http://localhost:9080. There you can create Main class, CBT build,
add libraries, plugins, readme and other things. Let's say you choose `my-project` as the project name.
The GUI will create `~/my-projects/my-project` for you.


Plugin-author guide
--------------------

A CBT plugin is a trait that is mixed into a Build class.
Only use this trait only for wiring things together.
Don't put logic in there. Instead simply call methods
on a separate class or object which serves as a library
for your actual logic. It should be callable and testable
outside of a Build class. This way the code of your plugin
will be easier to test and easier to re-use. Feel free
to make your logic rely on CBT's logger.

 See `plugins/` for examples.

Scala.js support
----------------

CBT supports cross-project Scala.js builds.
It preserves same structure as in sbt (https://www.scala-js.org/doc/project/cross-build.html)

 1. Example for user scalajs project is in: `$CBT_HOME/cbt/examples/build-scalajs`
 2. `$CBT_HOME/cbt compile`
    Will compile JVM and JS sources
    `$CBT_HOME/cbt jsCompile`
    Will compile JS sources
    `$CBT_HOME/cbt jvmCompile`
    Will compile JVM sources
 3. `$CBT_HOME/cbt fastOptJS` and `$CBT_HOME/cbt fullOptJS`
    Same as in Scala.js sbt project

 Note: Scala.js support is under ongoing development.

 Currently missing features:
 * No support for jsDependencies:
   It means that all 3rd party dependencies should added manually, see scalajs build example
 * No support for test


CBT productivity hacks
----------------------

only show first 20 lines of type errors to catch the root ones
```
cbt c 2>&1 | head -n 20
```

Inheritance Pittfalls
---------------------
```
trait Shared extends BaseBuild{
  // this lowers the type from Seq[Dependency] to Seq[DirectoryDependency]
  override def dependencies = Seq( DirectoryDependency(...) )
}
class Build(...) extends Shared{
  // this now fails because GitDependency is not a DirectoryDependency
  override def dependencies = Seq( GitDependency(...) )
}
// Solution: raise the type explicitly
trait Shared extends BaseBuild{
  // this lowers the type from Seq[Dependency] to Seq[DirectoryDependency]
  override def dependencies: Seq[Dependency] = Seq( DirectoryDependency(...) )
}
```

```
trait Shared extends BaseBuild{
  override def dependencies: Seq[Dependency] = Seq() // removes all dependencies, does not inclide super.dependencies
}
trait SomePlugin extends BaseBuild{
  // adds a dependency
  override def dependencies: Seq[Dependency] = super.dependencies ++ Seq( baz )
}
class Build(...) extends Shared with SomePlugin{
  // dependencies does now contain baz here, which can be surprising
}
// Solution can be being careful about the order and using traits instead of classes for mixins
class Build(...) extends SomePlugin with Shared
```
