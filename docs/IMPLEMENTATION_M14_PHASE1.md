# M14 Phase 1 implementation

## Scope and ownership

Phase 1 establishes only the Java 8-neutral foundations of `ZBW-PROG-006..008/014`,
`ZBW-CONTENT-007/009/011`, `ZBW-ADDON-026..040`, `ZBW-ADDON-274..282` (M14 profile/calendar
portion) and `ZBW-ADDON-369..378`. It extends `zbw-progression` and reuses M12 reward,
entitlement and storage contracts plus M13 quest/achievement identities.

Statistics (M15), PlaceholderAPI (M16), replay (M17), Atlas/moderation (M18), distributed
transport (M19/M20), external providers (M21) and compatibility adapters (M22) remain excluded.

## Foundation

- Typed cosmetic, category, rarity and calendar-campaign IDs use the shared namespaced identity
  contract.
- Immutable cosmetic definitions carry versioned category, rarity, localization, trigger and
  optional M12 entitlement/M13 quest/achievement unlock references.
- Immutable loadout and private profile settings snapshots carry optimistic revisions and audit
  metadata.
- Calendar campaigns reference existing M12 reward identities and validate bounded time windows.
- `M14Catalog` validates duplicate IDs and category/rarity references. `M14Configuration` enforces
  the 300-definition production gate and bounded effect/entity budgets.
- Repository and service ports accept caller-owned M04/M12 `UnitOfWork`, `RecordRevision` and
  `IdempotencyKey`; no new persistence or presentation framework is introduced.

## Deferred M14 work

Concrete persistence, ownership lifecycle, equipment policy, runtime effects, 300 approved
definitions, profile/calendar application services, M09 commands/GUI and Paper projection remain
later M14 phases.
