# M11.1 corrective implementation and certification

**Status:** COMPLETED and remotely certified; squash-merged to `main` on 2026-07-19
**Requirements:** `ZBW-GAME-004`, `ZBW-GAME-005`, `ZBW-SHOP-001..007`,
`ZBW-CONTENT-002..003`, `ZBW-READY-004`, `ZBW-READY-015`, and the M11-owned portions of
`ZBW-ADDON-010..025`, `061..070`, `141..147`, `184..201`, `236..244`, `300..322`,
`341..349`, `363..368`, `379..397`, `438..452`.

The governance-only exception record `docs/M11_1_MERGE_EXCEPTION.md` preserves the completed local
evidence from the temporary runner outage and does not falsely report those unavailable runs as
successful. PR #18 later executed all mandatory jobs successfully and was squash-merged as
`3e68835c361216e6dc8be37b9e024734bb565884`; that later evidence closes RC-087.

## Corrective result

M11.1 implements the RC-087 corrective scope without entering M12. Phase 1 materialized the Java-8-neutral,
disabled-by-default declarative scripting engine and transactional versioned M11 configuration
activation. Phase 2 completed the twelve named-mode/addon mechanics families through M08 lifecycle
snapshots, M10 mode bindings and deterministic M11 configuration.

The final integration sprint adds:

- `AtomicInventoryPurchasePort`, which validates and mutates match-local inventory and tender state
  as one owner-thread transaction with capacity checks, rollback and idempotent commit handling;
- `JdbcShopStateStore`, a cycle-free M04 SQL adapter for Quick Buy, favourites, purchase history and
  local rotation state with optimistic revisions, duplicate recovery, bounded history and restart
  recovery;
- lifecycle-safe coordination of generators, upgrades, utility actions and mode mechanics without
  duplicating the M08 state machine;
- the 25 M11 actions bound to M09 command/GUI definitions through `M11PresentationBindings`;
- `BukkitM11Platform`, an exact Java 21 Paper translation adapter for generator drops, forge
  delivery, reversible utility blocks, sounds, particles, entities and deterministic cleanup;
- mandatory Paper 1.21.1 build 133 certification and artifact evidence through
  `tools/validation/m11_paper_e2e.py` and the dedicated GitHub Actions workflow.

## Persistence and dependency direction

The former test-only cycle was removed by deleting arena/game dependencies on the concrete SQL
implementation. Generic SQL contracts remain tested in `zbw-storage-sql`; production dependency
direction is now `zbw-storage-sql -> zbw-shop`, while `zbw-shop -> zbw-arena -> zbw-storage-api`
never points back to the SQL adapter. No M12 persistent virtual-currency ledger is present.

## Acceptance evidence

Deterministic local suites cover scripting policy/security, configuration reload and last-known-good
retention, balance simulations, all named mechanics, atomic purchases, restart persistence,
duplicate/conflict recovery, lifecycle cleanup and M09 parity. The locked CI certification fails
unless all mandatory Paper assertions are true: shop inventory, item delivery, generator spawning,
duplicate prevention, reversible blocks, particles, sounds, forge/team effects, owner-thread
execution and cleanup.

Final publication evidence consists of the complete Java 8/11/16/17/21 matrix, clean Java 21
reactor, Checkstyle, SpotBugs, JaCoCo, strict JavaDoc, binary compatibility, dependency/licence/SBOM,
governance, traceability, addon catalogue, dashboard and Paper certification reports. No mandatory
test is permitted to be skipped. The local exception evidence recorded 346 tests with zero
failures/errors/skips, 36/36 governance tests, 49 addons/473 mappings, 672 dashboard rows and passing
JavaDoc, API compatibility and dependency/licence/SBOM gates. PR #18 later supplied the missing
remote matrices, including exact Paper 1.21.1 certification, on the immutable merged revision.

## Deferred ownership

M12 progression and persistent currencies, M15 statistics, M16 PlaceholderAPI, M17 replay, M18
Atlas, M19/M20 distributed coordination and proxy delivery, M21 external providers and M22 legacy
compatibility remain unchanged and incomplete. M11 completion only closes M11-owned cells.
