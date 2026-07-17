# M09 public API

## Exported packages

- `io.zartra.bedwars.command.api` — `CommandModel`, `CommandFramework` and
  `PresentationActions`. It is Java 8 and platform-neutral.
- `io.zartra.bedwars.ui.api` — `UiModel`, `UiFramework`, `ConfirmationFramework`,
  `EditorFramework`, `AdminDashboard` and `PresentationParity`. It is Java 8 and
  platform-neutral.
- `io.zartra.bedwars.command.paper` — Java 21 translation and bounded execution adapter.
- `io.zartra.bedwars.ui.paper` — Java 21 inventory translation adapter.

All public types and exported packages have strict JavaDoc. Parameters are non-null unless an
`Optional` or explicit empty value is documented. Validation errors are typed values or documented
`IllegalArgumentException`; operational failures become structured results. API callbacks may
complete asynchronously and must not assume a Minecraft owner thread. Paper mutations are confined
to the Paper adapter owner-thread port.

## Versioning and extension lifecycle

The M02 API-version and deprecation rules apply. Extensions register immutable command nodes,
page definitions and exact action bindings during bootstrap; duplicate identifiers fail closed.
Registries are frozen for execution. Shutdown stops admission, expires sessions/tokens and drains
bounded supervisors. Binary signature validation covers both neutral API packages.

The action binding contract is deliberately small: `PresentationActions.UseCase` accepts a neutral,
authorized request and returns a structured response. M07/M08 composition adapters implement this
contract; extensions cannot receive a Bukkit sender, inventory or internal implementation class.
