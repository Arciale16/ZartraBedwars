# M07 arena and map lifecycle

Arena and map identity is a collision-resistant typed UUID and never a display
name. Rename changes both display snapshots at one revision while retaining
every ID and semantic reference. Create rejects duplicate arena IDs, map IDs
and case-insensitive display names at the repository boundary.

Deep duplication produces a new arena ID and map ID, copies all immutable
teams, generators, NPCs, regions, holograms, modes, rules, speeds and metadata,
maps the requested target world explicitly and starts disabled at revision
zero. Source and copy share no mutable collection.

Enable is a guarded transition: the complete validator must have no error.
Successful valid snapshots may become last-known-good images. Disable does not
erase setup. Recovery restores only an exact-revision last-known-good image.
Delete, restore and world operations require authorization, target scope,
cancellable pre-events and audit evidence.

Rotation weight is bounded to 1–100000, priority to -100000–100000 and arena
inventory capacity by `ArenaPolicy`. Operational views expose configured,
invalid, active-setup and world-failure counts without player or map secrets.

Persistence adapters must atomically store the arena/map aggregate, optimistic
revision and last-known-good image. M07 adds no SQL or migration; the M04 record
store is the certified adapter boundary.
