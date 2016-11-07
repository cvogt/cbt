Eval - Scala runtime code compilation and evaluation

The code for Eval originally comes out of https://github.com/twitter/util . History was transferred to see origin and author of the changes in this file, but history was re-written to eliminate file renames.

The code was moved rather than a dependency on twitter/util added for these reasons:
- With minor changes the dependency on twitter `util-core` can be removed, which allows CBT to avoid adding it as a dependency
- According to @ryanoneill, Eval is deprecated within twitter (https://github.com/twitter/util/pull/179)
