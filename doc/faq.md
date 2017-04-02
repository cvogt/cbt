## How to fix my abstract method errors or other linking errors?

If the types in the error involve Scala standard library types,
chances are you are mixing dependencies using different Scala versions.
Be aware that `ScalaVersion` is bound to the scalaVersion of the Build
class it is called in. Transferring those objects can easily lead
to version conflicts. We'll try to detect or prevent these in the
future: https://github.com/cvogt/cbt/issues/478

It's also possible that you've hit a bug in CBT, but linking related
bugs have been rare since early 2017.
