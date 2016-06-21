This example shows integration with scalariform plugin.

Reformat executed on every `cbt compile` call, and affects only *.scala source files.

You can provide your custom scalariform preferences in build via `scalariformPreferences`.

To see formatting in action: execute `cbt breakFormatting` to break formatting and then execute `cbt scalariformFormat` to get formatting back.

To check if your code is properly formatted(for example as part of CI validation), you can execute:

```
cbt scalariformFormat
git diff --exit-code
```

Last command will return non-zero code, if your code isn't properly formatted.
