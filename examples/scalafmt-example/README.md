This example shows integration with scalafmt plugin.
Reformat executed on every `cbt compile` call, and affects only *.scala source files.
You can provide your custom scalfmt preferences in build via `scalafmtConfig`.
You can examine if your source code properly aligned already by executing `cbt scalafmtTest`
To test formatting in action you can execute: `cbt breakFormatting` to break formatting
and `cbt scalafmt` to get formatting back.
