# Team and arena configuration hardening

## Authoritative limits

| Value | Bound | Meaning |
|---|---:|---|
| Team count | 2–64 | Playable teams in one arena/match. |
| Per-team capacity | 1–64 | Players assigned to one team. |
| Match maximum players | 1–256 | Global admission safety ceiling. |

The constants in `TeamLayoutLimits` are safety constraints, not supported-layout
presets. Arena validation also requires `maximumPlayers <= teamCount * teamSize` and
the arena team size to lie inside the map's declared range. This removes the prior
arena-64/runtime-32 and arena-256/team-64 inconsistencies without imposing eight teams
as a universal ceiling.

Each `ArenaTeam` supplies a namespaced ID, display label and semantic color. The
assembler copies those values plus `ArenaDefinition.teamSize()` into `TeamDefinition`.
Assignment, admission, elimination and victory use identities and membership, never
list indexes or a fixed color list. Paper receives the resulting immutable projection
and makes no team decision.

## Defaults

`ArenaDefinition.builder(id, mapId, name, instant)` uses the original
`ArenaDefaultProfile.standard()`. The overload accepting a profile is the integration
point for the M03 configuration layer and later M09 editor. A profile contains the
world adapter, group, modes, min/max players, team size, selection weights and world
height defaults. Construction copies all values, so changing a registered profile
does not mutate existing arenas.

Operators may later replace or extend profiles through validated M03 configuration and
APIs. M08.1 does not add raw file reads, reload commands or GUI editors.
