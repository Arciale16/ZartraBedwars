# M07 arena, map and setup API reference

## Contract rules

All M07 types compile to Java 8, reject unexpected `null`, expose immutable
snapshots and report expected failures through typed `Result` values. Durable
port calls may block and must run on an M05 bounded worker. No public type
contains Bukkit, Paper, SQL, filesystem, Redis, proxy, command or GUI types.

| Package | Main contracts | Responsibility |
|---|---|---|
| `io.zartra.bedwars.arena.model` | `ArenaDefinition`, `MapDefinition`, `ArenaBundle`, locations, regions, teams, generators, NPCs and holograms | Immutable configuration and stable identity |
| `io.zartra.bedwars.arena.validation` | `ArenaValidation.Validator`, `Report`, `Issue` | Deterministic complete validation and enable gating |
| `io.zartra.bedwars.arena.setup` | `SetupSession`, `SetupMutation`, `SetupPreview`, `MarkerProposal`, `SetupToolDefinition` | Revision-fenced draft lifecycle and two-phase changes |
| `io.zartra.bedwars.arena.spi` | Arena/setup/archive repositories, atomic commit, identity, marker, event and audit ports | Provider-neutral integration boundaries |
| `io.zartra.bedwars.arena.application` | Arena, setup, archive and world lifecycle services; policy, health, events and failures | Authorized use cases and orchestration |
| `io.zartra.bedwars.arena.archive` | `ArenaArchive`, `ArenaArchiveCodec`, `CanonicalArenaArchiveCodec` | Bounded deterministic metadata interchange |

## Versioning and threading

`build/api-signature-baseline-m07.txt` is additive over M06. Incompatible
changes require the established deprecation and major-version policy. Values,
reports and events are safe to pass between threads after construction.
Repository, codec and marker-provider calls execute on workers. World methods
return asynchronous cancellable handles and never wait on the caller.

## Error and lifecycle model

`ArenaFailures` distinguishes forbidden, not-found, conflict, invalid,
capacity, stale-preview, archive and world failures. Programming errors such as
negative revisions or malformed bounded values throw immediately. Authorization
and cancellable pre-events run before mutation; immutable completion events and
audit facts are emitted only with the final typed outcome.

M09 presentation adapters must invoke these services and may not duplicate
arena rules. M16 may project authorized fields; M21 may implement world
providers; M22 completes legacy runtime rendering and certification.
