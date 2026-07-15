# M07 import, export, backup and restore

The schema-one archive contains only bounded arena/map metadata. It has stable
archive, arena and map IDs, creation time, schema version, a maximum 4 MiB
payload and a lower-case SHA-256 checksum. The codec has explicit collection
bounds, rejects duplicate identities, trailing bytes, malformed values,
unsupported schemas and envelope/payload identity mismatches, and never uses
Java object deserialization.

Export creates a deterministic envelope without storage mutation. Backup saves
that envelope through `ArenaArchiveStore`. Import preserves source arena/map
IDs and therefore fails on collision. Restore requires an exact current
revision and matching target ID. An enabled imported snapshot is accepted only
if current validation still permits enable.

World files are deliberately absent. M06/M21 world providers own filesystem
clone/reset; M07 archives cannot carry executable content or arbitrary paths.
All codec and store work belongs on bounded workers. Authorization, pre-events,
typed failures, audit and immutable completion events apply to every mutating
operation.

No M07 database migration was necessary. Backup metadata is stored through the
existing M04 transactional record facilities; MySQL/MariaDB SQL regressions are
required again only when a later change modifies schema or repository SQL.
