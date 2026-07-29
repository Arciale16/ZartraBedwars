# M06 world-provider foundation

## Operation pipeline

1. `WorldOperation` validates typed identity, target, optional source and total
   deadline.
2. `WorldOrchestrator` admits work only while running, below the configured
   in-flight bound and with no active operation for the same target.
3. The provider returns an immutable plan of at most 16 steps without doing I/O.
4. Worker-affinity steps execute through the bounded M05 scheduler. Owner steps
   execute through `OwnerThreadDispatcher`.
5. Failure, cancellation or timeout compensates completed steps in reverse
   order and records whether every rollback succeeded.
6. The target lease and active accounting are released before the terminal
   completion stage is published, allowing deterministic same-world chaining.

Load creates/validates a directory then attaches the world. Clone copies a
template without `uid.dat` or `session.lock`, then attaches it. Reset unloads,
atomically moves the existing target to an operation-specific backup, copies
the template, reloads and discards the backup; copy failure restores the old
world. Unload detaches without synchronous filesystem deletion.

## Bounds and failure behavior

- In-flight operations: 1–64; tracked native worlds: 1–256.
- Scheduler workers and queue are bounded; saturation is typed rejection.
- Operation timeout is a total deadline; rollback steps receive a bounded
  five-second compensation deadline.
- Cancellation is cooperative for filesystem traversal and idempotent at the
  public handle.
- Drain stops admission, cancels active work and waits only for its supplied
  positive budget. Paper invokes blocking worker shutdown from a dedicated
  bounded off-owner executor.
- Resource snapshots contain only booleans/counters. An unloaded world is
  leak-free only with zero loaded chunks, entities and retained handles.

## Provider contract

Provider implementations must build plans without I/O, enforce affinity,
return non-null typed step results, make rollback idempotent and expose fast
secret-safe snapshots. Optional WorldEdit, FAWE, WorldGuard, Slime and Multiverse adapters are
materialized by M21 over this unchanged SPI; arena ownership/concurrency remains
M07. The native M06 implementation remains the safe fallback and no optional
provider receives world or lifecycle ownership. See `WORLD_PROVIDERS_M21.md`.
