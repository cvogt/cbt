Join the chat at https://gitter.im/cvogt/cbt

Welcome to Chris' Build Tool (CBT) for Scala
============================================

Fun, fast, intuitive, composable and statically checked builds written in Scala.

Currently CBT has been tested in OSX only. Adding support for Unix and Windows
should not be hard. Please contribute back if you mange :).

CBT supports the basic needs for Scala builds right now.
Composing, compiling, running, testing, packaging, publishing.
Tasks outside of these, such as building manuals will require
easy custom code. If you integrate something, consider
doing it as traits that you make available as a library that
other builds can depend on and mix in.

Slides and video from CBT talk from NEScala 2016:
https://github.com/cvogt/talks/raw/master/2016-03-04_NEScala-2016_A-Vision-For-Scala-Builds.pdf
https://www.youtube.com/watch?v=5HfKw3hgdOM

Getting started
---------------

### Dependencies
You currently need javac, nailgun, gpg and realpath or gcc installed.

### Installing
CBT bootstraps from source. To install, just clone the repository.

### Using
To use, just call the `cbt` bash script. You will see CBT first
building up itself, then trying to start your build.

The easiest CBT build requires no script. It compiles source files from the
current directory and from `src/` into the target folder. Try calling `cbt compile`.
If you have a class called Main with a main method, try `cbt run`.
Put a source file with a Main class into `test/` in order for
`cbt test` to run it. It will see your main code.

If you need more than this, like dependencies, create a scala file in `build/`
that describes your build. Here is an example

```scala
// build/build.scala
class Build(val context: cbt.Context) extends PackageJars {
  override def defaultVersion = "0.6.1"

  override def name = "play-json-extensions"

  override def groupId = "org.cvogt"

  override def dependencies =
    super.dependencies ++
      Resolver(mavenCentral).bind(
        // encouraged way to declare dependencies
        ScalaDependency("com.typesafe.play", "play-json", "2.4.4"),
        MavenDependency("joda-time", "joda-time", "2.9.2")
      )

  override def compile = {
    println("Compiling...")
    super.compile
  }

  def foo = "Hello World"
}
```

Dependencies could also be declared using SBT style.

```scala
class Build(val context: cbt.Context) extends PackageJars {
...
// sbt compatible dependencies definition
override def dependencies =
  super.dependencies ++
    Resolver(mavenCentral).bind(
      "com.typesafe.play" %% "play-json" % "2.4.4",
      "joda-time" % "joda-time" % "2.9.2"
    )
...
}      
```

Now you can call methods of this class through cbt. Try `cbt foo`.
You can see how your build is configured via overrides.

call `cbt` to see a full list of available commands for this build.

Look into the class PackageBuild (and it's super class BasicBuild) in CBT's source code to see their
details. The source code is really simple. Don't shy away from
looking, even as a beginner. No crazy stuff, I promise ;). You
can find the relevant code in CBT's stage2/BasicBuild.scala

I order to keep executing the same command triggered by file changes use `cbt loop <command>`.

You can find example builds in CBT's own `test/` folder.
Not all of them have a build file, in which case CBT uses the default
cbt.BasicBuild.

A folder `build/` can have its own folder `build/` inside in order
to add source or maven dependencies to your build. Eventually
you'll be able to also choose the CBT and Scala versions for
target builds. Make sure you extend cbt.BuilBuild instead of
cbt.Build, in order to automatically trigger building of the
target build.

cbt is fast. It uses Nailgun to keep the JVM hot. It uses the Java
WatchService (respectively a fast OSX port of it) for instant triggering
re-compilation on file changes. Use `cbt loop compile`.

CBT concepts
------------

There two essential primitives available in build scripts for composing
modular projects:

  1. Dynamically compiling and loading Build scripts in other
     directories and calling methods (aka tasks) on them to compile,
     get the classpath, ask for version numbers, etc.

     This allows to do a lot of things just like that:
     Multi-project builds, source dependencies, builds of builds and
     allowing tests simply as dependent projects of the main project, etc.

  2. Maven dependencies

     I wrote my own 50 LOC Maven resolver. It's super quick and I have
     yet see it not to being able to handle something. I know cases
     exist, but seem rare.
     alexarchambault's Coursier can be used as a more complete drop-in.

Build scripts also have access to a small unsurprising library for
- triggering dependencies to build / download and get the classpath
- compiling Java / Scala code using zinc with given class paths
- running code
- packaging jars
- signing / publishing to sonatype/maven

Scala.js support
----------------

CBT supports cross-project Scala.js build.
It preserves same structure as in SBT (https://www.scala-js.org/doc/project/cross-build.html)

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

Missing features in comparison with SBT
---------------------------------------

Not implemented yet, but rather easily possible without API changes or
major refactors is concurrently building dependencies and
running tests. Right now it is sequential.
Maven downloads already happen in parallel as well as some tasks like packaging.

Another edge case that may need a solution is dynamically overwriting
tasks. SBT allows that. Classes and traits are static. The only use
cases I know are debugging, cross builds and the sbt release plugin. A
solution could be code generating traits at build-time and mixing them
in ad-hoc. It's a build-tool after all. Build-time code-generation and
class loading is not rocket science. But there may be simpler solutions
for the cases at hand. And they are edge cases anyways.

Bash completions
----------------
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

Fish shell completions
----------------
copy this line into your fish configuration, on OSX: /.config/fish/config.fish
complete -c cbt -a '(cbt taskNames)'

Zsh completions
---------------
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
