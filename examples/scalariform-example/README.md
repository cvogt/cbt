This example shows integration with scalariform plugin.
Reformat executed on every `cbt compile` call, and affects only *.scala source files.
You can provide your custom scalariform preferences in build via `scalariformPreferences`.
To test formatting in action you can execute: `cbt breakFormatting` to break formatting
and `cbt scalariformReformat` to get formatting back.
