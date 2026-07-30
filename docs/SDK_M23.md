# Extension SDK and example

The public SDK remains Java 8 and platform independent. Extensions use `Extension`, immutable
`ExtensionMetadata`, generic `Extension.Point<T>` and typed public contracts. They receive no
Bukkit, storage, filesystem, secret or implementation object.

`ExampleMigrationExtension` is a compiled example distributed by `zbw-sdk`. It demonstrates:

- immutable marketplace metadata and compatible API/product/Minecraft ranges;
- asynchronous start, bounded drain and stop;
- declaration of a public capability;
- a deterministic `MigrationApi.Provider`;
- data-only conversion with an explicit unsupported result.

The matching descriptor is
`examples/example-migration-extension.properties` inside the SDK artifact. Installation never edits
core. Metadata validation must succeed before the entrypoint is loaded. Duplicate IDs, unsupported
API ranges, missing required dependencies and duplicate declarations remain fail-closed.

API removal requires a new major range. Existing members remain binary compatible throughout the
published supported major window; deprecation documentation must name its replacement.

`SuggestionProvider` is advisory-only and disabled until composed by an operator-approved adapter.
Its results are untrusted, require owner-side validation and cannot authorize enforcement or mutate
state.
