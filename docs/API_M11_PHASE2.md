# M11 Phase 2 API — generator and resource foundation

**Status:** implemented and verified Phase 2 boundary; M11 remains active  
**Requirements:** `ZBW-SHOP-006`, mechanics portions of `ZBW-ADDON-194..201` and
`ZBW-ADDON-363..368`

## Boundary

`io.zartra.bedwars.shop.generator` is Java 8-neutral and contains no Bukkit, Paper, NMS,
filesystem, JDBC, Redis, proxy or runtime-configuration dependency. Paper adapters later translate
confirmed delivery batches into platform effects; they may not own generator policy.

The implementation consumes immutable M07 `ArenaDefinition` and M08 `MatchSnapshot` values. It
does not recreate either lifecycle. `GeneratorFleet.start` accepts only `PLAYING`, and a
non-playing tick permanently cleans its match-local runtimes.

## Configuration and state

`GeneratorConfiguration` defines stable identity/type/resource, arena or team ownership, interval,
capacity, amount, enabled state and delivery rule. Any valid `ResourceId` supports a custom
resource; canonical iron, gold, diamond and emerald identities already exist in the tender registry.

`ArenaGeneratorPlan` projects sorted arena placements and applies explicit validated per-arena
overrides. Unknown override IDs fail validation. `GeneratorState` reports `STOPPED`, `RUNNING` or
`CLEANED`, sequence, pending units and the next logical generation instant.

## Generation and delivery

`GeneratorRuntime` is synchronized, deterministic and bounded to 128 generation/delivery attempts
per invocation. Each batch has a stable match/generator/sequence `IdempotencyKey`. Capacity is
checked before queue admission; retries remain queued, while delivered/already-delivered results
acknowledge the batch. Cleanup removes all match-local pending state.

`ResourceDeliveryPort` implementations must atomically apply a batch and fence its idempotency key
within the owner-thread resource boundary. They return `RETRY` on transient failure and must not
report success before the platform effect is committed.

## Generator Split and deferred ownership

`GeneratorSplitPolicy` sorts eligible player IDs, rejects duplicates and rotates indivisible
remainders by sequence. Later M11 composition supplies team/alive/distance/AFK eligibility and
primary-Paper effects. PlaceholderAPI remains M16, distributed synchronization M19/M20, hologram
providers M21 and legacy mappings/full compatibility M22.
