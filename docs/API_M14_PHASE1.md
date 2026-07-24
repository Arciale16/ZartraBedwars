# M14 Phase 1 API

The M14 foundation is Java 8 neutral and additive to the immutable M13 progression API.

- `cosmetic`: typed IDs, versioned definitions, rarities and revisioned loadout snapshots.
- `profile`: private-by-default profile/effect preferences with revision and audit metadata.
- `calendar`: versioned reward windows using M12 `RewardId` values.
- `catalog.M14Catalog` and `M14Configuration`: deterministic reference/count/budget validation.
- `repository.CosmeticStateRepository` and `ProfileSettingsRepository`: caller-owned transaction,
  optimistic revision and idempotency contracts.
- `application.M14Service`: neutral query/mutation boundary intended for later M09 adapters.

All repository calls may perform I/O and therefore must run off owner/tick threads. Implementations
must return results to platform owners through the existing scheduler boundary.
