# M12 Phase 3 API

The exported Java-8 packages are:

- `io.zartra.bedwars.progression.experience` — XP rules and calculated awards.
- `io.zartra.bedwars.progression.level` — versioned level formulas and previews.
- `io.zartra.bedwars.progression.prestige` — prestige tiers and transaction intents.
- `io.zartra.bedwars.progression.reward` — reward definitions, plans, outputs, atomic claims,
  delivery outcomes, retries and compensation.
- `io.zartra.bedwars.progression.entitlement` — generic unlock thresholds.
- `io.zartra.bedwars.progression.application` — authorized administrative use cases and atomic
  mutation ports.
- `io.zartra.bedwars.progression.integration` — configured M08/M11 input mappings.
- `io.zartra.bedwars.progression.projection.ProgressionProjectionService` — M04 transaction-owned
  projection and recovery coordination.

Public values reject null or malformed state at construction. Expected policy rejection uses
`IllegalArgumentException` or `IllegalStateException`; authorization denial uses `SecurityException`;
durable failures remain typed `Result` values at storage/projection ports. No public type exposes
Bukkit, Paper, NMS, JDBC, SQL or a mutable global singleton.
