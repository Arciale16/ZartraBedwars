# M10 API reference

All neutral M10 types target Java 8, are immutable where practical and contain no platform or
storage objects. Exact exported signatures are locked in `build/api-signature-baseline-m10.txt`.

| Package | Contract families |
|---|---|
| `io.zartra.bedwars.game.mode` | stable mode/version IDs, schema fields, layouts, deferred bindings, immutable registry/events/migration |
| `io.zartra.bedwars.game.selector` | typed candidates/queries/pages/selections; Team Selector; Compass tracker/callouts |
| `io.zartra.bedwars.game.matchmaking` | requests, parties, queues, diagnostics, policies, arena facts, reservations, M08 assignment port |
| `io.zartra.bedwars.game.spectator` | sessions, preferences, restrictions, targets, lifecycle/restoration/events |
| `io.zartra.bedwars.command.api` | additive `PresentationActions.Catalog.m10()` and `throughM10()` |
| `io.zartra.bedwars.paper.game` | Java 21 owner-thread projection and bounded off-owner executor |

Nulls are rejected at public boundaries. Validation failures use `IllegalArgumentException`,
stale/terminal state uses `IllegalStateException`, and authorization denial uses
`SecurityException`. Matching extensions receive immutable inputs and must remain bounded and
deterministic. M10 registration does not declare named-mode gameplay implemented.
