# M12 Storage Plan

## Implemented Phase 2 boundary

The storage boundary is implemented in `zbw-storage-sql` through checksum-locked schema version 12 and adapters for all eight `zbw-progression` repository ports. The migration creates `progression_accounts`, `progression_xp_ledger`, `progression_level_history`, `progression_prestige_history`, `currency_accounts`, `economic_transactions`, `economic_transaction_entries`, `reward_grants`, `reward_deliveries`, `reward_failures`, `progression_unlocks`, and the M11 bridge `purchase_settlements`.

M04 `zbw_schema_history`, inbox/outbox, recovery, retention, legal-hold, tombstone, and backup tables are reused rather than duplicated. Transaction boundaries are: inbox claim + progression revision + XP ledger + optional reward intent + outbox; balance revision + immutable economic transaction/entry; and purchase reference + economic transaction + settlement record. Unique idempotency keys and optimistic revisions provide restart-safe replay and duplicate prevention. Pending reward-delivery tables are schema foundations only; execution belongs to a later M12 phase.

SQLite is the deterministic local contract authority. The approved M04 container workflow remains the certification path for exact MySQL and MariaDB images.
