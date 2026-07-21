# M13 Phase 1 implementation

## Scope

M13 Phase 1 establishes the Java 8 platform-neutral foundation for `ZBW-PROG-009`,
`ZBW-PROG-010`, `ZBW-PROG-012`, `ZBW-PROG-013`, `ZBW-CONTENT-004..006` and the foundational
portions of `ZBW-ADDON-081..091`.

No new module was materialized because the approved module graph assigns M13 progression policy to
`zbw-progression`. The existing M12 module now contains these additional packages:

- `progression.objective`: typed event/objective/filter/scope/composition definitions, immutable
  bounded progress and an optimistic repository port;
- `progression.quest`: schedules, claim policy, quest definitions, assignment lifecycle and repository port;
- `progression.achievement`: immutable monotonic tier definitions;
- `progression.challenge`: typed timed challenge variants;
- `progression.pass`: versioned season windows and free/premium reward tiers;
- `progression.catalog`: immutable definition graph and deterministic duplicate/reference validation.

All collections are defensively copied, all public identities are typed, all definition versions and
time/value bounds are validated, and reward references reuse the completed M12 `RewardId` contract.

## Explicitly deferred

Phase 1 does not implement event consumers, SQL/JDBC, migrations, live assignment/claim services,
starter catalogue activation, M09 commands/GUI, Paper projections or notifications. It does not
advance statistics (M15), PlaceholderAPI (M16), replay (M17), Atlas (M18), distributed/proxy systems
(M19/M20), external providers (M21) or compatibility adapters (M22).

## Verification

`M13FoundationTest` covers typed IDs, immutability, malformed values, objective composition,
filters/scopes, monotonic bounded progress, immediate duplicate handling, quest assignment bounds,
achievement tier monotonicity, challenge duration, pass season/track validation and catalogue
duplicate/reference rejection. Architecture/governance checks prove the Java 8 neutral boundary and
preserve the 672-requirement and 473-addon mapping baselines.

`api_compatibility_m13.py` records the exact additive Phase 1 public surface and independently proves
that every immutable M12 progression signature remains present.
