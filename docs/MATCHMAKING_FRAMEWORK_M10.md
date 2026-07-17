# Matchmaking framework M10

`QueueService` atomically indexes a solo actor or complete party under an idempotency key. Limits
bound queue count, requests per queue and total actors. Revision, cancellation token, deadline,
party leader/membership revision, locale, region and trace identity are explicit inputs.

`FairCapacityPolicy` orders stable inputs, applies bounded aging, never splits a party and returns
structured `Decision` evidence. `ArenaAvailability` admits only an enabled, healthy, world-ready,
joinable, non-recovering arena with compatible mode/layout and safe capacity. Reservations have
one owner, revision, expiry, confirmation and release. Assignment crosses `AssignmentPort` into
the existing M08 use case. Cross-server queues and durable proxy recovery remain M20.
