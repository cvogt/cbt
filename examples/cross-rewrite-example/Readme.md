This is an example of how to use scalafix rewrite rules to produce multiple
variants of the same code base for different scala versions, e.g. 2.11.8 and
2.12.1 and against different comparable libraries, e.g. cats and scalaz.

To package the jars for all combinations run `cbt cross.package`.
