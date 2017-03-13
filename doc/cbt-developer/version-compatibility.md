## Why does CBT version compatiblity matter?
CBT allows fixing the version used using the `// cbt: ` annotation.
This means that the CBT version you installed will download the
CBT version referenced there and use that instead. CBT also
allows composing builds, which require different CBT versions.
In order for all of this to work, different CBT versions need to
be able to talk to one another.

This chapter is about how the current solution works and about
the pitfalls involved which can make a new version of CBT
incompatible with older versions.

## How can compatibility be broken?
The current solution mostly relies on the Java interfaces in
the `compability/` folder. Changing the Java interface in a
non-backwards-compatible way means making the CBT incompatible with
older versions. Java 8 default methods make this a whole lot easier
and this is the main reason CBT relies on Java 8. But we also
want to keep this interfaces as small as possible in order to
minimize the risk.

However there are more things that can break compatibility when changed:
- the format of the `// cbt: ` version string
- the name and format of Build classes (or files) that CBT looks for
- communication between versions via reflection in particular
  - how the TrapSecurityManager of each CBT version talks to the
    installed TrapSecurityManager via reflection
- communication via the file system
  - cache folder location any layout
  - .cbt-loop.tmp file

## How to detect accidental breakages?

CBT's tests have a few tests with `// cbt: ` annotations and some
reference libraries as Git dependencies that use these annotations.
Many incompatibilities will lead to these tests failing, either with
a compilation error against the `compatiblity/` interfaces or with
a runtime exception or unexpected behavior in case other things are
broken.

## How an we improve the situation in the long run?

In the long run we should think about how to reduce the risk
or sometimes even unavoidability of incompatibilities.
The unavoidability mostly stems from limitations of what Java
interfaces can express. However Java 8 interfaces mostly work
fairly well. We should consider using them for the cases that
currently use reflection instead of reflection.

If Java 8 interfaces still turn out to be a problem in the long run,
we could consider an interface that we control completely, e.g.
an internal serialization formation, which CBT versions use to talk
to each other.
