# M08 player-state restoration

Admission captures an immutable inventory, semantic world location, coordinates,
rotation, game mode and visibility. The captured value is retained for the session and
is never reconstructed from a gameplay inventory.

The Paper projector clears/replaces owned inventory slots, resolves worlds/items through
version-safe adapters, validates item type and exact amount, teleports, restores mode and
visibility, and updates inventory only on the primary thread. Restoration is idempotent
in the aggregate and runs after durable completion; exit, failure and reset paths share
the same restoration contract.
