This example's main method simply prints the current process id.
This can be used to experiment with cbt's restart feature
(the equivalent to sbt-revolver).

```
cbt direct loop restart
```

starts the main method in a separate process and kills it, when
a change in project, build file or cbt is detected.
