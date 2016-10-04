This example shows different use-cases for JVM system properties as they implemented in cbt.

### Pass custom properties inside task

Custom properties passed by user are available via `context.props`, all system properties can be gathered regular way via `System.getProperty`

```scala
def showProps: Unit = {
  val customProps: Map[String, String] = context.props
  val foo: Option[String] = customProps.get("foo")
  val bar: Option[String] = customProps.get("bar")

  println(s"=== Custom properties, foo: ${foo}, bar: ${bar}")

  val timezone: String = System.getProperty("user.timezone")
  val os: String = System.getProperty("os.name")

  println(s"=== System properties, timezone: ${timezone}, os: ${os}")
}
```

Here is output of task execution:

```
➜  system-properties ✗ cbt showProps -Dfoo=bar
=== Custom properties, foo: Some(bar), bar: None
=== System properties, timezone: Europe/Moscow, os: Mac OS X
```

You can also override system default properties by passing your own:

```
➜  system-properties ✗ cbt showProps -Dfoo=bar -Duser.timezone=UTC
=== Custom properties, foo: Some(bar), bar: None
=== System properties, timezone: UTC, os: Mac OS X
```

### Pass custom properties to use from code

You can also pass and use properties inside your code:

```scala
object Main extends App {
  val foo: Option[String] = Option(System.getProperty("foo"))
  val bar: Option[String] = Option(System.getProperty("bar"))

  println(s"=== Custom properties, foo: ${foo}, bar: ${bar}")

  val timezone: String = System.getProperty("user.timezone")
  val os: String = System.getProperty("os.name")

  println(s"=== System properties, timezone: ${timezone}, os: ${os}")
}
```

You pass properties to `cbt run` in the same fashion:

```
➜  system-properties ✗ cbt run -Dfoo=run-run-run -Dbar=stop
=== Custom properties, foo: Some(run-run-run), bar: Some(stop)
=== System properties, timezone: , os: Mac OS X
```

And override them too:

```
➜  system-properties git:(system-properties) ✗ cbt run -Dfoo=run-run-run -Dbar=stop -Duser.timezone=UTC
=== Custom properties, foo: Some(run-run-run), bar: Some(stop)
=== System properties, timezone: UTC, os: Mac OS X
```

### Get scala REPL with colors :rainbow:

This one is dead-simple. Colorized REPL is only possible to get with `-Dscala.color` flag:

```
cbt repl -Dscala.color
```

will give you nicer view of REPL.
