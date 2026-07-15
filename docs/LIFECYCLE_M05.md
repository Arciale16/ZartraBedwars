# M05 lifecycle and recovery guide

`LifecycleCoordinator` submits control work to an injected bounded scheduler. Startup follows
declared dependency order. A failed or invalid startup result rolls back every started component in
reverse order. Shutdown stops admission through component drain, then stops in reverse order;
deadline exhaustion or a component failure invokes its bounded force-stop path. Failures are
isolated in an immutable report and the coordinator reaches an explicit terminal state.

Components are unique by stable ID, idempotent and receive the remaining transition budget. Their
methods run only on the lifecycle worker. They must not block Minecraft owner threads and must not
create unbounded executors or queues.

`RecoveryCoordinator` is the M05 foundation of ZBW-GAME-010. It runs ordered idempotent steps over
an injected durable compare-and-set `MarkerStore`: DETECTED, QUIESCED, PLAYERS_ROUTED, RECONCILED
and exactly-once RECOVERED. Invalid, incomplete, failed or conflicting recovery persists
MANUAL_REQUIRED where the store is still available, publishes a structured administrator-visible
failure and never reports silent completion. Marker revision and `IdempotencyKey` fence duplicate
completion. M08 supplies actual match persistence, player routing and gameplay restoration steps;
M05 contains no gameplay or storage adapter.
